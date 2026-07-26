package com.cbtpulsegrid.backend.institution;

import java.util.UUID;

public interface InstitutionProfileQuery {

	InstitutionProfile requireProfile(UUID institutionId);
}
