import sharp from 'sharp';
import {GetObjectCommand, S3Client} from '@aws-sdk/client-s3';

const s3Client = new S3Client({region: 'ap-northeast-2'});
const SOURCE_BUCKET = process.env.SOURCE_BUCKET;
const ALLOWED_WIDTHS = process.env.ALLOWED_WIDTHS?.split(',').map(Number) || [200, 800, 1920];

/**
 * Lambda Function URL Handler
 * CloudFront Origin으로 동작하여 이미지 동적 리사이징 수행
 *
 * URL 형식: https://{function-url}/{s3Key}?width=200&format=auto
 *
 * @param {Object} event - Lambda Function URL 이벤트
 * @returns {Object} HTTP 응답 (이미지 바이너리)
 */
export const handler = async (event) => {
    // 모든 요청 로깅 (디버깅용)
    console.log('🔍 Lambda invoked with event:', JSON.stringify(event, null, 2));

    try {
        // 1. 요청 파싱
        const {s3Key, width, format} = parseRequest(event);

        // 2. S3에서 원본 이미지 다운로드
        const originalImage = await downloadFromS3(s3Key);

        // 3. Sharp로 이미지 리사이징
        const resizedImage = await resizeImage(originalImage, width, format);

        // 4. HTTP 응답 반환 (Base64 인코딩)
        return {
            statusCode: 200,
            headers: {
                'Content-Type': getContentType(format),
                'Cache-Control': 'public, max-age=86400', // 24시간 캐싱
                'X-Resized-Width': String(width)
            },
            body: resizedImage.toString('base64'),
            isBase64Encoded: true
        };

    } catch (error) {
        console.error('Image resizing error:', error);
        return handleError(error);
    }
};

/**
 * Lambda Function URL 이벤트에서 요청 파라미터 추출
 */
function parseRequest(event) {
    // rawPath: "/{s3Key}" 형식
    const s3Key = event.rawPath?.substring(1); // 첫 번째 '/' 제거

    if (!s3Key) {
        throw new Error('S3 key is required');
    }

    if (s3Key.includes('..') || s3Key.startsWith('/')) {
        throw new Error('Invalid S3 key');
    }

    // Query String 파싱
    const params = new URLSearchParams(event.rawQueryString || '');
    const width = parseInt(params.get('width') || '800');
    const format = params.get('format') || 'auto';

    // width 검증
    if (!ALLOWED_WIDTHS.includes(width)) {
        throw new Error(`Invalid width. Allowed: ${ALLOWED_WIDTHS.join(', ')}`);
    }

    console.log(`Resizing request: s3Key=${s3Key}, width=${width}, format=${format}`);

    return {s3Key, width, format};
}

/**
 * S3에서 원본 이미지 다운로드
 */
async function downloadFromS3(s3Key) {
    try {
        const command = new GetObjectCommand({
            Bucket: SOURCE_BUCKET,
            Key: s3Key
        });

        const response = await s3Client.send(command);

        // Stream을 Buffer로 변환
        const chunks = [];
        for await (const chunk of response.Body) {
            chunks.push(chunk);
        }

        return Buffer.concat(chunks);

    } catch (error) {
        if (error.name === 'NoSuchKey') {
            throw new Error('Image not found in S3');
        }
        throw error;
    }
}

/**
 * Sharp를 사용한 이미지 리사이징
 *
 * - WebP quality 90 (프로필 사진, 피부톤 디테일 보존)
 * - effort 6 (최대 압축 효율, 파일 크기 최소화)
 * - smartSubsample: false (피부톤 색상 왜곡 방지)
 * - mozjpeg: true (JPEG 압축 최적화)
 *
 * @param {Buffer} imageBuffer - 원본 이미지
 * @param {number} width - 목표 너비 (px)
 * @param {string} format - 출력 형식 ('auto', 'webp', 'jpeg')
 * @returns {Promise<Buffer>} 리사이징된 이미지
 */
async function resizeImage(imageBuffer, width, format) {
    let pipeline = sharp(imageBuffer)
        .rotate() // EXIF Orientation 자동 처리
        .resize(width, null, {
            fit: 'inside',            // 비율 유지하며 리사이징
            withoutEnlargement: true, // 원본보다 크게 만들지 않음
            kernel: 'lanczos3'        // 최고 품질 축소 알고리즘 (명시적 지정)
        });

    // 형식 변환 - 프로필 사진 최적화
    if (format === 'webp' || format === 'auto') {
        pipeline = pipeline.webp({
            quality: 90,              // 85 → 90 (피부톤, 얼굴 디테일 10-15% 개선)
            effort: 6,                // 최대 압축 효율 (파일 크기 10-20% 감소)
            smartSubsample: false     // 크로마 서브샘플링 개선 (피부톤 품질)
        });
    } else if (format === 'jpeg') {
        pipeline = pipeline.jpeg({
            quality: 90,              // 85 → 90 (일관성)
            progressive: true,
            mozjpeg: true             // Sharp 0.33+ 지원 (JPEG 압축 최적화)
        });
    } else {
        // 기본값: 고품질 JPEG
        pipeline = pipeline.jpeg({
            quality: 90,
            progressive: true,
            mozjpeg: true
        });
    }

    return pipeline.toBuffer();
}

/**
 * 형식에 따른 Content-Type 반환
 */
function getContentType(format) {
    const contentTypes = {
        'webp': 'image/webp',
        'jpeg': 'image/jpeg',
        'jpg': 'image/jpeg',
        'png': 'image/png',
        'auto': 'image/webp' // 기본값
    };
    return contentTypes[format] || 'image/webp';
}

/**
 * 에러 처리
 */
function handleError(error) {
    const errorMessage = error.message || 'Internal server error';

    if (errorMessage.includes('not found')) {
        return {
            statusCode: 404,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({error: 'Image not found'})
        };
    }

    if (errorMessage.includes('Invalid width')) {
        return {
            statusCode: 400,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({error: errorMessage})
        };
    }

    return {
        statusCode: 500,
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({error: 'Image processing failed'})
    };
}
