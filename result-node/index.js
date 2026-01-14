import express from 'express';
import pkg from 'pg';

const { Pool } = pkg;

const app = express();
const port = process.env.PORT || 3000;

// Database configuration (from env vars)
const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  user: process.env.DB_USER || 'earthmars',
  password: process.env.DB_PASSWORD || 'earthmars_password',
  database: process.env.DB_NAME || 'earthmarsdb'
};

const pool = new Pool(dbConfig);

/**
 * Health check
 */
app.get('/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok' });
  } catch (err) {
    res.status(500).json({ status: 'db_error' });
  }
});

/**
 * Get voting results
 */
app.get('/results', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT choice, COUNT(*) AS count
      FROM votes
      GROUP BY choice
    `);

    const response = { earth: 0, mars: 0 };

    for (const row of result.rows) {
      response[row.choice] = Number(row.count);
    }

    res.json(response);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch results' });
  }
});

app.listen(port, () => {
  console.log(`Result service running on port ${port}`);
});
