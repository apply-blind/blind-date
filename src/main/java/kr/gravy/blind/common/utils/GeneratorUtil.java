package kr.gravy.blind.common.utils;

import com.fasterxml.uuid.Generators;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class GeneratorUtil {

    public UUID generatePublicId() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}
