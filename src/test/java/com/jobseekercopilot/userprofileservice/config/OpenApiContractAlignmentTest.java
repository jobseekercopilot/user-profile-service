package com.jobseekercopilot.userprofileservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceWriteRequest;
import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
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
        JsonNode contact = contract.path("components")
                .path("schemas")
                .path("ProfessionalContact")
                .path("properties");
        assertEquals(
                ProfileConstraints.MAX_PROFESSIONAL_PHONE_LENGTH,
                contact.path("phone").path("maxLength").asInt());
        assertEquals(
                ProfileConstraints.MAX_PROFESSIONAL_LINKS,
                contact.path("links").path("maxItems").asInt());
        JsonNode link = contract.path("components")
                .path("schemas")
                .path("ProfessionalLink")
                .path("properties");
        assertEquals(
                ProfileConstraints.MAX_PROFESSIONAL_LINK_LABEL_LENGTH,
                link.path("label").path("maxLength").asInt());
        assertEquals(
                ProfileConstraints.MAX_PROFESSIONAL_LINK_URL_LENGTH,
                link.path("url").path("maxLength").asInt());
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
