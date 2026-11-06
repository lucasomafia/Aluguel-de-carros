import { useEffect, useRef, useState } from 'react'

async function chamarApi(caminho, metodo, autorizacao, dados) {
  let resposta
  try {
    resposta = await fetch(`/api/${caminho}`, {
      method: metodo,
      credentials: 'omit',
      signal: AbortSignal.timeout(10000),
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
        ...(autorizacao ? { Authorization: autorizacao } : {}),
      },
      ...(dados ? { body: JSON.stringify(dados) } : {}),
    })
  } catch {
    throw new Error('Não foi possível conectar ao sistema. Tente novamente em instantes.')
  }

  const resultado = resposta.status === 204 ? null : await resposta.json().catch(() => null)
  if (!resposta.ok) {
    if (resposta.status === 401) throw new Error('Usuário ou senha incorretos.')
    if (resposta.status === 409) throw new Error(resultado?.detail || 'Este registro já está cadastrado.')
    if (resposta.status >= 500) throw new Error('O sistema está indisponível no momento. Tente novamente mais tarde.')
    throw new Error(resultado?.campos?.join(' • ') || resultado?.detail || 'Não foi possível concluir a operação.')
  }
  return resultado
}

const dadosVazios = { rg: '', cpf: '', endereco: '', profissao: '', vinculos: [] }
const automovelVazio = { matricula: '', ano: '', marca: '', modelo: '', placa: '' }
const filtrosVazios = { busca: '', marca: '', ano: '' }

function App() {
  const [tela, definirTela] = useState('inicio')
  const [nome, definirNome] = useState('')
  const [nomeUsuario, definirNomeUsuario] = useState('')
  const [senha, definirSenha] = useState('')
  const [usuario, definirUsuario] = useState(null)
  const [cliente, definirCliente] = useState(null)
  const [formularioCliente, definirFormularioCliente] = useState(dadosVazios)
  const [automoveis, definirAutomoveis] = useState([])
  const [carregandoLista, definirCarregandoLista] = useState(true)
  const [erroAutomoveis, definirErroAutomoveis] = useState(null)
  const [filtros, definirFiltros] = useState(filtrosVazios)
  const [filtrosAplicados, definirFiltrosAplicados] = useState(filtrosVazios)
  const [formularioAutomovel, definirFormularioAutomovel] = useState(automovelVazio)
  const [automovelEmEdicao, definirAutomovelEmEdicao] = useState(null)
  const [carregando, definirCarregando] = useState(false)
  const [mensagem, definirMensagem] = useState(null)
  const autorizacao = useRef(null)
  const cadastro = tela === 'cadastro'
  const perfis = { CLIENTE: 'Cliente', EMPRESA: 'Empresa', BANCO: 'Banco' }
  const marcas = [...new Set(automoveis.map((automovel) => automovel.marca))].sort((primeira, segunda) => primeira.localeCompare(segunda, 'pt-BR'))
  const anos = [...new Set(automoveis.map((automovel) => automovel.ano))].sort((primeiro, segundo) => segundo - primeiro)
  const automoveisFiltrados = automoveis.filter((automovel) => {
    const busca = filtrosAplicados.busca.trim().toLocaleLowerCase('pt-BR')
    return (!busca || `${automovel.marca} ${automovel.modelo} ${automovel.placa} ${automovel.matricula}`.toLocaleLowerCase('pt-BR').includes(busca))
      && (!filtrosAplicados.marca || automovel.marca === filtrosAplicados.marca)
      && (!filtrosAplicados.ano || String(automovel.ano) === filtrosAplicados.ano)
  })
  const filtrosAtivos = Boolean(filtrosAplicados.busca || filtrosAplicados.marca || filtrosAplicados.ano)

  useEffect(() => {
    let ativo = true
    chamarApi('automoveis', 'GET', null)
      .then((lista) => { if (ativo) definirAutomoveis(lista) })
      .catch((erro) => { if (ativo) definirErroAutomoveis(erro.message) })
      .finally(() => { if (ativo) definirCarregandoLista(false) })
    return () => { ativo = false }
  }, [])

  function mostrarMensagem(erro, texto) { definirMensagem({ erro, texto }) }

  function trocarTela(novaTela) {
    definirTela(novaTela)
    definirMensagem(null)
    definirSenha('')
  }

  async function carregarCliente() {
    try {
      const dados = await chamarApi('clientes/me', 'GET', autorizacao.current)
      definirCliente(dados)
      definirFormularioCliente({ rg: dados.rg, cpf: dados.cpf, endereco: dados.endereco, profissao: dados.profissao,
        vinculos: dados.vinculos.map((vinculo) => ({ ...vinculo, rendimento: String(vinculo.rendimento) })) })
    } catch (erro) {
      if (erro.message.includes('ainda não cadastrados')) {
        definirCliente(null)
        definirFormularioCliente(dadosVazios)
      } else {
        mostrarMensagem(true, erro.message)
      }
    }
  }

  async function carregarAutomoveis() {
    definirCarregandoLista(true)
    definirErroAutomoveis(null)
    try {
      definirAutomoveis(await chamarApi('automoveis', 'GET', autorizacao.current))
    } catch (erro) {
      definirErroAutomoveis(erro.message)
    } finally {
      definirCarregandoLista(false)
    }
  }

  async function enviarAcesso(evento) {
    evento.preventDefault()
    definirMensagem(null)
    if (cadastro && new TextEncoder().encode(senha).length > 72) {
      mostrarMensagem(true, 'A senha deve ocupar no máximo 72 bytes.')
      return
    }
    definirCarregando(true)
    try {
      if (cadastro) {
        await chamarApi('auth/cadastro', 'POST', null, { nome, nomeUsuario, senha })
        definirTela('login')
        definirNome('')
        definirSenha('')
        mostrarMensagem(false, 'Conta criada! Entre com seu usuário e senha.')
      } else {
        const bytes = new TextEncoder().encode(`${nomeUsuario}:${senha}`)
        const credencial = `Basic ${btoa(String.fromCharCode(...bytes))}`
        const dadosUsuario = await chamarApi('auth/login', 'POST', credencial)
        autorizacao.current = credencial
        definirUsuario(dadosUsuario)
        definirTela('conta')
        definirSenha('')
        if (dadosUsuario.perfil === 'CLIENTE') await carregarCliente()
        await carregarAutomoveis()
      }
    } catch (erro) {
      mostrarMensagem(true, erro.message)
    } finally {
      definirCarregando(false)
    }
  }

  async function consultarUsuario() {
    definirCarregando(true)
    definirMensagem(null)
    try {
      definirUsuario(await chamarApi('auth/me', 'GET', autorizacao.current))
      if (usuario.perfil === 'CLIENTE') await carregarCliente()
      await carregarAutomoveis()
      mostrarMensagem(false, 'Dados atualizados com sucesso.')
    } catch (erro) {
      mostrarMensagem(true, erro.message)
    } finally {
      definirCarregando(false)
    }
  }

  function atualizarCampo(campo, valor) {
    definirFormularioCliente((atual) => ({ ...atual, [campo]: valor }))
  }

  function atualizarVinculo(indice, campo, valor) {
    definirFormularioCliente((atual) => ({ ...atual, vinculos: atual.vinculos.map((vinculo, posicao) =>
      posicao === indice ? { ...vinculo, [campo]: valor } : vinculo) }))
  }

  async function salvarCliente(evento) {
    evento.preventDefault()
    definirMensagem(null)
    definirCarregando(true)
    try {
      const dados = { ...formularioCliente, cpf: formularioCliente.cpf.replace(/\D/g, ''),
        vinculos: formularioCliente.vinculos.map((vinculo) => ({ ...vinculo, rendimento: Number(vinculo.rendimento) })) }
      const salvo = await chamarApi('clientes/me', 'PUT', autorizacao.current, dados)
      definirCliente(salvo)
      definirFormularioCliente({ ...salvo, vinculos: salvo.vinculos.map((vinculo) => ({ ...vinculo, rendimento: String(vinculo.rendimento) })) })
      mostrarMensagem(false, 'Dados do cliente salvos com sucesso.')
    } catch (erro) {
      mostrarMensagem(true, erro.message)
    } finally {
      definirCarregando(false)
    }
  }

  async function excluirCliente() {
    if (!window.confirm('Excluir seus dados de cliente? Sua conta de acesso continuará ativa.')) return
    definirCarregando(true)
    definirMensagem(null)
    try {
      await chamarApi('clientes/me', 'DELETE', autorizacao.current)
      definirCliente(null)
      definirFormularioCliente(dadosVazios)
      mostrarMensagem(false, 'Dados do cliente excluídos.')
    } catch (erro) {
      mostrarMensagem(true, erro.message)
    } finally {
      definirCarregando(false)
    }
  }

  async function salvarAutomovel(evento) {
    evento.preventDefault()
    definirCarregando(true)
    definirMensagem(null)
    try {
      const dados = { ...formularioAutomovel, ano: Number(formularioAutomovel.ano) }
      await chamarApi(automovelEmEdicao ? `automoveis/${automovelEmEdicao}` : 'automoveis',
        automovelEmEdicao ? 'PUT' : 'POST', autorizacao.current, dados)
      definirFormularioAutomovel(automovelVazio)
      definirAutomovelEmEdicao(null)
      await carregarAutomoveis()
      mostrarMensagem(false, automovelEmEdicao ? 'Automóvel atualizado.' : 'Automóvel cadastrado.')
    } catch (erro) {
      mostrarMensagem(true, erro.message)
    } finally {
      definirCarregando(false)
    }
  }

  function editarAutomovel(automovel) {
    definirAutomovelEmEdicao(automovel.id)
    definirFormularioAutomovel({ matricula: automovel.matricula, ano: String(automovel.ano),
      marca: automovel.marca, modelo: automovel.modelo, placa: automovel.placa })
    definirMensagem(null)
  }

  async function excluirAutomovel(automovel) {
    if (!window.confirm(`Excluir ${automovel.marca} ${automovel.modelo}?`)) return
    definirCarregando(true)
    definirMensagem(null)
    try {
      await chamarApi(`automoveis/${automovel.id}`, 'DELETE', autorizacao.current)
      if (automovelEmEdicao === automovel.id) {
        definirAutomovelEmEdicao(null)
        definirFormularioAutomovel(automovelVazio)
      }
      await carregarAutomoveis()
      mostrarMensagem(false, 'Automóvel excluído.')
    } catch (erro) {
      mostrarMensagem(true, erro.message)
    } finally {
      definirCarregando(false)
    }
  }

  function sair() {
    autorizacao.current = null
    definirUsuario(null)
    definirCliente(null)
    definirFormularioCliente(dadosVazios)
    definirFormularioAutomovel(automovelVazio)
    definirAutomovelEmEdicao(null)
    definirNome('')
    definirNomeUsuario('')
    definirSenha('')
    definirMensagem(null)
    definirTela('inicio')
  }

  if (tela === 'inicio') {
    return (
      <main className="pagina-inicial">
        <header className="cabecalho-inicial">
          <button type="button" className="marca-inicial" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>Aluguel de carros</button>
          <nav aria-label="Acesso"><button type="button" onClick={() => trocarTela(usuario ? 'conta' : 'login')}>{usuario ? 'Minha conta' : 'Entrar'}</button></nav>
        </header>
        <section className="destaque-inicial">
          <div className="texto-destaque"><h1>Seu próximo caminho começa aqui.</h1><p>Explore os automóveis cadastrados e encontre o que combina com você.</p><button type="button" onClick={() => trocarTela(usuario ? 'conta' : 'cadastro')}>{usuario ? 'Minha conta' : 'Criar conta'}</button></div>
          <img src="/carro-home.png" alt="Vista da estrada de dentro de um carro" />
        </section>
        <form className="barra-filtros" onSubmit={(evento) => { evento.preventDefault(); definirFiltrosAplicados({ ...filtros }) }} aria-label="Filtrar automóveis">
          <label htmlFor="filtro-busca">Buscar automóvel<input id="filtro-busca" type="search" placeholder="Marca, modelo ou placa" value={filtros.busca} onChange={(evento) => definirFiltros((atual) => ({ ...atual, busca: evento.target.value }))} /></label>
          <label htmlFor="filtro-marca">Marca<select id="filtro-marca" value={filtros.marca} onChange={(evento) => definirFiltros((atual) => ({ ...atual, marca: evento.target.value }))}><option value="">Todas as marcas</option>{marcas.map((marca) => <option key={marca} value={marca}>{marca}</option>)}</select></label>
          <label htmlFor="filtro-ano">Ano<select id="filtro-ano" value={filtros.ano} onChange={(evento) => definirFiltros((atual) => ({ ...atual, ano: evento.target.value }))}><option value="">Todos os anos</option>{anos.map((ano) => <option key={ano} value={ano}>{ano}</option>)}</select></label>
          <button type="submit">Buscar</button>
        </form>
        <section className="catalogo-inicial" aria-labelledby="titulo-catalogo">
          <div className="cabecalho-catalogo"><div><h2 id="titulo-catalogo">Automóveis</h2><p>{filtrosAtivos ? `${automoveisFiltrados.length} resultado${automoveisFiltrados.length === 1 ? '' : 's'} encontrado${automoveisFiltrados.length === 1 ? '' : 's'}.` : 'Veja os veículos cadastrados no sistema.'}</p></div><div className="acoes-catalogo">{filtrosAtivos && <button type="button" className="secundario" onClick={() => { definirFiltros(filtrosVazios); definirFiltrosAplicados(filtrosVazios) }}>Limpar filtros</button>}<button type="button" className="secundario" onClick={carregarAutomoveis} disabled={carregandoLista}>Atualizar lista</button></div></div>
          {erroAutomoveis && <p className="mensagem erro" role="alert">{erroAutomoveis}</p>}
          {carregandoLista ? <p className="estado-lista">Carregando automóveis...</p> : !erroAutomoveis && automoveisFiltrados.length === 0 ? <p className="estado-lista">{automoveis.length ? 'Nenhum automóvel corresponde aos filtros.' : 'Nenhum automóvel cadastrado ainda.'}</p> : null}
          <div className="grade-automoveis">{automoveisFiltrados.map((automovel) => <article className="cartao-automovel" key={automovel.id}><span className="selo-ano">{automovel.ano}</span><h3>{automovel.marca} {automovel.modelo}</h3><p>Placa {automovel.placa}</p><p>Matrícula {automovel.matricula}</p></article>)}</div>
        </section>
      </main>
    )
  }

  return (
    <main className={`pagina ${usuario ? 'pagina-larga' : ''} ${!usuario ? 'pagina-acesso' : ''}`}>
      {!usuario && <aside className="painel-ilustracao" aria-label="Imagem de automóvel"><img src="/carro-estrada.png" alt="Carro em uma estrada cercada por árvores" /></aside>}
      <div className="conteudo-pagina">
      <header className="cabecalho">
        <button type="button" className="voltar-inicio" onClick={() => trocarTela('inicio')}>← Início</button>
        <h1>Aluguel de carros</h1>
        <p>{usuario ? 'Minha conta' : 'Cadastro e acesso ao sistema'}</p>
      </header>
      <section className="cartao" aria-labelledby="titulo">
        {usuario ? (
          <>
            <div className="topo-conta"><div><h2 id="titulo">Olá, {usuario.nome}!</h2><p className="descricao">{perfis[usuario.perfil] || usuario.perfil} · {usuario.nomeUsuario}</p></div><button type="button" className="secundario" onClick={sair}>Sair</button></div>
            {mensagem && <p className={`mensagem ${mensagem.erro ? 'erro' : 'sucesso'}`} role={mensagem.erro ? 'alert' : 'status'}>{mensagem.texto}</p>}
            {usuario.perfil === 'CLIENTE' ? (
              <>
                <div className="titulo-secao"><div><h3>Meus dados</h3><p className="descricao">{cliente ? 'Atualize seu cadastro quando precisar.' : 'Complete seu cadastro de cliente.'}</p></div><button type="button" className="secundario" onClick={consultarUsuario} disabled={carregando}>Atualizar</button></div>
                <form onSubmit={salvarCliente}>
                  <fieldset disabled={carregando}>
                    <div className="grade-campos">
                      <label htmlFor="rg">RG<input id="rg" value={formularioCliente.rg} onChange={(evento) => atualizarCampo('rg', evento.target.value)} required maxLength={30} /></label>
                      <label htmlFor="cpf">CPF<input id="cpf" inputMode="numeric" value={formularioCliente.cpf} onChange={(evento) => atualizarCampo('cpf', evento.target.value)} required pattern="[0-9]{11}" maxLength={11} title="Informe os 11 dígitos do CPF" /></label>
                      <label htmlFor="endereco">Endereço<input id="endereco" autoComplete="street-address" value={formularioCliente.endereco} onChange={(evento) => atualizarCampo('endereco', evento.target.value)} required maxLength={255} /></label>
                      <label htmlFor="profissao">Profissão<input id="profissao" value={formularioCliente.profissao} onChange={(evento) => atualizarCampo('profissao', evento.target.value)} required maxLength={120} /></label>
                    </div>
                    <div className="titulo-secao vinculos-titulo"><div><h3>Rendimentos</h3><p className="descricao">Informe até três entidades empregadoras.</p></div>{formularioCliente.vinculos.length < 3 && <button type="button" className="secundario" onClick={() => definirFormularioCliente((atual) => ({ ...atual, vinculos: [...atual.vinculos, { entidadeEmpregadora: '', rendimento: '' }] }))}>Adicionar</button>}</div>
                    {formularioCliente.vinculos.map((vinculo, indice) => <div className="linha-vinculo" key={indice}>
                      <label htmlFor={`empresa-${indice}`}>Entidade empregadora<input id={`empresa-${indice}`} value={vinculo.entidadeEmpregadora} onChange={(evento) => atualizarVinculo(indice, 'entidadeEmpregadora', evento.target.value)} required maxLength={120} /></label>
                      <label htmlFor={`rendimento-${indice}`}>Rendimento mensal (R$)<input id={`rendimento-${indice}`} type="number" min="0" step="0.01" value={vinculo.rendimento} onChange={(evento) => atualizarVinculo(indice, 'rendimento', evento.target.value)} required /></label>
                      <button type="button" className="secundario remover" onClick={() => definirFormularioCliente((atual) => ({ ...atual, vinculos: atual.vinculos.filter((_, posicao) => posicao !== indice) }))}>Remover</button>
                    </div>)}
                    <div className="acoes"><button type="submit">{carregando ? 'Salvando...' : 'Salvar dados'}</button>{cliente && <button type="button" className="secundario" onClick={excluirCliente}>Excluir meus dados</button>}</div>
                  </fieldset>
                </form>
              </>
            ) : <button type="button" onClick={consultarUsuario} disabled={carregando}>Atualizar dados</button>}
            <div className="titulo-secao secao-automoveis"><div><h3>Automóveis</h3><p className="descricao">{automoveis.length ? `${automoveis.length} veículo${automoveis.length === 1 ? '' : 's'} cadastrado${automoveis.length === 1 ? '' : 's'}.` : 'Nenhum automóvel cadastrado.'}</p></div><button type="button" className="secundario" onClick={carregarAutomoveis} disabled={carregando}>Atualizar lista</button></div>
            {usuario.perfil !== 'CLIENTE' && <form onSubmit={salvarAutomovel} className="formulario-automovel"><fieldset disabled={carregando}>
              <h3>{automovelEmEdicao ? 'Editar automóvel' : 'Cadastrar automóvel'}</h3>
              <div className="grade-campos">
                <label htmlFor="matricula">Matrícula<input id="matricula" value={formularioAutomovel.matricula} onChange={(evento) => definirFormularioAutomovel((atual) => ({ ...atual, matricula: evento.target.value }))} required maxLength={40} /></label>
                <label htmlFor="placa">Placa<input id="placa" value={formularioAutomovel.placa} onChange={(evento) => definirFormularioAutomovel((atual) => ({ ...atual, placa: evento.target.value.toUpperCase() }))} required maxLength={7} pattern="[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}" title="Use o formato ABC1234 ou ABC1D23" /></label>
                <label htmlFor="marca">Marca<input id="marca" value={formularioAutomovel.marca} onChange={(evento) => definirFormularioAutomovel((atual) => ({ ...atual, marca: evento.target.value }))} required maxLength={80} /></label>
                <label htmlFor="modelo">Modelo<input id="modelo" value={formularioAutomovel.modelo} onChange={(evento) => definirFormularioAutomovel((atual) => ({ ...atual, modelo: evento.target.value }))} required maxLength={80} /></label>
                <label htmlFor="ano">Ano<input id="ano" type="number" min="1886" max={new Date().getFullYear() + 1} value={formularioAutomovel.ano} onChange={(evento) => definirFormularioAutomovel((atual) => ({ ...atual, ano: evento.target.value }))} required /></label>
              </div>
              <div className="acoes"><button type="submit">{automovelEmEdicao ? 'Salvar alterações' : 'Cadastrar automóvel'}</button>{automovelEmEdicao && <button type="button" className="secundario" onClick={() => { definirAutomovelEmEdicao(null); definirFormularioAutomovel(automovelVazio) }}>Cancelar edição</button>}</div>
            </fieldset></form>}
            <div className="lista-automoveis">{automoveis.map((automovel) => <article className="item-automovel" key={automovel.id}>
              <div><strong>{automovel.marca} {automovel.modelo}</strong><p>{automovel.ano} · Placa {automovel.placa} · Matrícula {automovel.matricula}</p></div>
              {usuario.perfil !== 'CLIENTE' && <div className="acoes"><button type="button" className="secundario" onClick={() => editarAutomovel(automovel)} disabled={carregando}>Editar</button><button type="button" className="secundario" onClick={() => excluirAutomovel(automovel)} disabled={carregando}>Excluir</button></div>}
            </article>)}</div>
          </>
        ) : (
          <>
            <nav className="opcoes" aria-label="Acesso"><button type="button" className={cadastro ? '' : 'selecionado'} aria-pressed={!cadastro} onClick={() => trocarTela('login')} disabled={carregando}>Entrar</button><button type="button" className={cadastro ? 'selecionado' : ''} aria-pressed={cadastro} onClick={() => trocarTela('cadastro')} disabled={carregando}>Criar conta</button></nav>
            <h2 id="titulo">{cadastro ? 'Crie sua conta' : 'Entre na sua conta'}</h2>
            <p className="descricao">{cadastro ? 'Cadastre-se como cliente para acessar o sistema.' : 'Informe seu usuário e senha para continuar.'}</p>
            {mensagem && <p className={`mensagem ${mensagem.erro ? 'erro' : 'sucesso'}`} role={mensagem.erro ? 'alert' : 'status'}>{mensagem.texto}</p>}
            <form onSubmit={enviarAcesso}><fieldset disabled={carregando}>
              {cadastro && <label htmlFor="nome">Nome completo<input id="nome" name="nome" autoComplete="name" value={nome} onChange={(evento) => definirNome(evento.target.value)} required maxLength={120} /></label>}
              <label htmlFor="nomeUsuario">Usuário<input id="nomeUsuario" name="nomeUsuario" autoComplete="username" autoCapitalize="none" spellCheck={false} value={nomeUsuario} onChange={(evento) => definirNomeUsuario(evento.target.value)} required minLength={3} maxLength={60} pattern="[a-zA-Z0-9._-]{3,60}" /></label>
              {cadastro && <small>Use letras sem acentos, números, ponto, hífen ou sublinhado.</small>}
              <label htmlFor="senha">Senha<input id="senha" name="senha" type="password" autoComplete={cadastro ? 'new-password' : 'current-password'} value={senha} onChange={(evento) => definirSenha(evento.target.value)} required minLength={cadastro ? 8 : undefined} maxLength={64} /></label>
              {cadastro && <small>De 8 a 64 caracteres, com no máximo 72 bytes.</small>}
              <button className="enviar" type="submit">{carregando ? 'Aguarde...' : cadastro ? 'Criar conta' : 'Entrar'}</button>
            </fieldset></form>
          </>
        )}
      </section>
      </div>
    </main>
  )
}

export default App
