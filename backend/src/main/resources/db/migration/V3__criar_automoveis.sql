CREATE TABLE automoveis (
    id UUID PRIMARY KEY,
    matricula VARCHAR(40) NOT NULL UNIQUE,
    ano INTEGER NOT NULL CHECK (ano >= 1886),
    marca VARCHAR(80) NOT NULL,
    modelo VARCHAR(80) NOT NULL,
    placa VARCHAR(10) NOT NULL UNIQUE,
    proprietario_id UUID NOT NULL REFERENCES usuarios(id)
);

ALTER TABLE automoveis ENABLE ROW LEVEL SECURITY;
