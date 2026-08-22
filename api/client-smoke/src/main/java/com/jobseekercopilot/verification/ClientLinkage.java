package com.jobseekercopilot.verification;

import com.jobseekercopilot.generated.userprofileservice.api.UserProfilesApi;
import com.jobseekercopilot.generated.userprofileservice.client.ApiClient;

final class ClientLinkage {

    private ClientLinkage() {
    }

    static UserProfilesApi createClient() {
        return new UserProfilesApi(new ApiClient());
    }
}
