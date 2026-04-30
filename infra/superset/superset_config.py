import os

SQLALCHEMY_DATABASE_URI = os.environ.get(
    "SQLALCHEMY_DATABASE_URI",
    "postgresql+psycopg2://stablepay:stablepay_dev@postgres:5432/superset",
)

SECRET_KEY = os.environ.get(
    "SUPERSET_SECRET_KEY",
    "stablepay-dev-secret-key-not-for-production",
)

SQLLAB_TIMEOUT = 30
SUPERSET_WEBSERVER_TIMEOUT = 60
