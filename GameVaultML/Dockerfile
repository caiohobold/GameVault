FROM python:3.12-slim

WORKDIR /app

# torch so para CPU: a versao padrao do Linux traz CUDA e passa de 2 GB
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY scripts ./scripts
COPY dados_demo ./dados_demo

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
