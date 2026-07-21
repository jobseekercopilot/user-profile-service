CREATE INDEX idx_profile_skills_profile_id ON user_profile_skills (user_profile_id);
CREATE INDEX idx_profile_target_roles_profile_id ON user_profile_target_roles (user_profile_id);
CREATE INDEX idx_qualification_profile_id ON qualification (user_profile_id);
CREATE INDEX idx_role_profile_id ON role (user_profile_id);
