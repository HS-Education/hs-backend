-- Generation fencing prevents delayed Azure messages from replacing a newer retry.
ALTER TABLE public.documents ADD COLUMN processing_generation integer NOT NULL DEFAULT 1;
