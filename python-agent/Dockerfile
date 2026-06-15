FROM python:3.12-slim

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --create-home app

COPY pyproject.toml requirements.lock ./
COPY app ./app
COPY agents ./agents
COPY models ./models
COPY prompts ./prompts

RUN pip install --no-cache-dir -r requirements.lock

USER app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]