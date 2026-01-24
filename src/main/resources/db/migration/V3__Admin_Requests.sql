-- ==============================================
-- V3__Admin_Requests.sql
-- Align DB schema with AdminRegisterRequest entity (admin_requests)
-- ==============================================

SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.admin_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP,
    CONSTRAINT chk_admin_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint
           WHERE conname = 'fk_admin_requests_user'
       )
    THEN
        ALTER TABLE public.admin_requests
            ADD CONSTRAINT fk_admin_requests_user
            FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_admin_requests_user_id ON public.admin_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_admin_requests_status ON public.admin_requests(status);
CREATE INDEX IF NOT EXISTS idx_admin_requests_pending_created_date
    ON public.admin_requests(created_date)
    WHERE status = 'PENDING';
