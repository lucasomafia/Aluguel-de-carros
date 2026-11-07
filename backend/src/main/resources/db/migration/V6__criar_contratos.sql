CREATE TABLE contratos (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL UNIQUE REFERENCES pedidos(id),
    proprietario_id UUID NOT NULL REFERENCES usuarios(id),
    tipo_propriedade VARCHAR(20) NOT NULL,
    executado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_contratos_proprietario ON contratos(proprietario_id);
ALTER TABLE contratos ENABLE ROW LEVEL SECURITY;
