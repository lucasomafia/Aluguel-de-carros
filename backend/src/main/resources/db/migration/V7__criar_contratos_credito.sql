CREATE TABLE contratos_credito (
    id UUID PRIMARY KEY,
    contrato_id UUID NOT NULL UNIQUE REFERENCES contratos(id),
    banco_id UUID NOT NULL REFERENCES usuarios(id),
    valor NUMERIC(14,2) NOT NULL CHECK (valor > 0),
    parcelas INTEGER NOT NULL CHECK (parcelas BETWEEN 1 AND 120),
    concedido_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_contratos_credito_banco ON contratos_credito(banco_id);
ALTER TABLE contratos_credito ENABLE ROW LEVEL SECURITY;
