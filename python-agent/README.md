# Python Agent

FastAPI, Pydantic, provider-neutral LLM gateway.

## Responsibilities

- `recommend-yield`, `parse-goal`, `summarize-audit`
- Java chart/data tools (HTTP) — no financial math
- Chart-request feedback backlog writes

## Dev (WP-5+)

```bash
pip install -e ".[dev]"
uvicorn app.main:app --reload --port 8000
pytest
```

## Prompts

Draft prompts live in gitignored `python-agent/prompts/`. WP-5 adds tracked `prompts/*.txt` copied from local planning.

## Config

`JAVA_API_URL`, `LLM_*`, `INTERNAL_API_KEY` — see `.env.example`. Never commit API keys.