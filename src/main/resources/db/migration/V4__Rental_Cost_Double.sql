-- ==============================================
-- V4__Rental_Cost_Double.sql
-- Align rental.cost column type with JPA mapping (Double)
-- ==============================================

SET search_path TO public;

DO $$
BEGIN
    IF to_regclass('public.rental') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'rental'
             AND column_name = 'cost'
       )
    THEN
        ALTER TABLE public.rental
            ALTER COLUMN cost TYPE DOUBLE PRECISION
            USING cost::double precision;

        ALTER TABLE public.rental
            ALTER COLUMN cost SET DEFAULT 0.0;
    END IF;
END $$;

