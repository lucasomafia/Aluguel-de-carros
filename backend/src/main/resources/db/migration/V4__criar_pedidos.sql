CREATE TABLE pedidos (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL REFERENCES usuarios(id),
    automovel_id UUID NOT NULL REFERENCES automoveis(id),
    data_retirada DATE NOT NULL,
    data_devolucao DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT datas_pedido_validas CHECK (data_devolucao > data_retirada)
);

CREATE INDEX idx_pedidos_cliente ON pedidos(cliente_id);
ALTER TABLE pedidos ENABLE ROW LEVEL SECURITY;
