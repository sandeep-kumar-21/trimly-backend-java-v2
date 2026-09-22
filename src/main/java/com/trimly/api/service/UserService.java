package com.trimly.api.service;

import com.trimly.api.dto.request.ChangePasswordRequest;
import com.trimly.api.dto.request.DeleteAccountRequest;
import com.trimly.api.dto.request.UpdatePreferencesRequest;
import com.trimly.api.dto.request.UpdateProfileRequest;
import com.trimly.api.dto.response.ExportJobResponse;
import com.trimly.api.dto.response.UserResponse;

public interface UserService {

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserResponse updatePreferences(Long userId, UpdatePreferencesRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    ExportJobResponse initiateDataExport(Long userId);

    ExportJobResponse getExportStatus(String jobId);

    void deleteAccount(Long userId, DeleteAccountRequest request);
}
