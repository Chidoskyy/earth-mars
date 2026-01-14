CREATE TABLE IF NOT EXISTS votes (
  id SERIAL PRIMARY KEY,
  choice TEXT NOT NULL CHECK (choice IN ('earth', 'mars')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_votes_choice ON votes(choice);
