CREATE TABLE clientes (
    usuario_id UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    rg VARCHAR(30) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    endereco VARCHAR(255) NOT NULL,
    profissao VARCHAR(120) NOT NULL
);

CREATE TABLE vinculos_empregaticios (
    usuario_id UUID NOT NULL REFERENCES clientes(usuario_id) ON DELETE CASCADE,
    posicao INTEGER NOT NULL CHECK (posicao BETWEEN 0 AND 2),
    entidade_empregadora VARCHAR(120) NOT NULL,
    rendimento NUMERIC(12,2) NOT NULL CHECK (rendimento >= 0),
    PRIMARY KEY (usuario_id, posicao)
);

ALTER TABLE clientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE vinculos_empregaticios ENABLE ROW LEVEL SECURITY;
