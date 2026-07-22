package com.cbtpulsegrid.backend.identity.account;

import java.util.Set;
import java.util.UUID;

import com.cbtpulsegrid.backend.identity.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 200) String password,
		UUID institutionId,
		@NotEmpty Set<@Valid @NotNull Role> roles,
		@Size(max = 100) String registrationNumber
) {
}
