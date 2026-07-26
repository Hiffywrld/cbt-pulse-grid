package com.cbtpulsegrid.backend.identity.account;

import com.cbtpulsegrid.backend.identity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
		@NotNull UserStatus status
) {
}
