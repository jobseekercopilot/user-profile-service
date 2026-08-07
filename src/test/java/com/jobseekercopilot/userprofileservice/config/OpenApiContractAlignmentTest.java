package com.jobseekercopilot.userprofileservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceWriteRequest;
import jakarta.validation.constraints.Size;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractAlignmentTest {

    @Test
    void runtimeMetadataAndWriteBoundsMatchProducerOwnedContract() throws Exception {
        Path moduleRoot = Path.of(OpenApiContractAlignmentTest.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI())
                .getParent()
                .getParent();
        JsonNode contract = new ObjectMapper().readTree(
                moduleRoot.resolve("api/openapi.json").toFile());
        String runtimeVersion = new OpenApiConfig()
                .userProfileOpenAPI()
                .getInfo()
                .getVersion();

        assertEquals(contract.path("info").path("version").asText(), runtimeVersion);
        assertWriteBound(contract, "description");
        assertWriteBound(contract, "responsibilities");
        assertWriteBound(contract, "achievements");
        assertEquals(
                50,
                contract.path("components")
                        .path("schemas")
                        .path("EvidenceSnapshotSelection")
                        .path("properties")
                        .path("facts")
                        .path("maxItems")
                        .asInt());
    }

    private void assertWriteBound(JsonNode contract, String fieldName) throws Exception {
        int modelBound = EvidenceWriteRequest.class
                .getDeclaredField(fieldName)
                .getAnnotation(Size.class)
                .max();
        int contractBound = contract.path("components")
                .path("schemas")
                .path("EvidenceWriteRequest")
                .path("properties")
                .path(fieldName)
                .path("maxLength")
                .asInt();
        assertEquals(contractBound, modelBound);
    }
}
