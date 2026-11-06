import { useRef, useState } from 'react'

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
    if (resposta.status === 409) throw new Error('Este usuário ou CPF já está cadastrado.')
    if (resposta.status >= 500) throw new Error('O sistema está indisponível no momento. Tente novamente mais tarde.')
    throw new Error(resultado?.campos?.join(' • ') || resultado?.detail || 'Não foi possível concluir a operação.')
  }
  return resultado
}

const dadosVazios = { rg: '', cpf: '', endereco: '', profissao: '', vinculos: [] }

function App() {
  const [tela, definirTela] = useState('login')
  const [nome, definirNome] = useState('')
  const [nomeUsuario, definirNomeUsuario] = useState('')
  const [senha, definirSenha] = useState('')
  const [usuario, definirUsuario] = useState(null)
  const [cliente, definirCliente] = useState(null)
  const [formularioCliente, definirFormularioCliente] = useState(dadosVazios)
  const [carregando, definirCarregando] = useState(false)
  const [mensagem, definirMensagem] = useState(null)
  const autorizacao = useRef(null)
  const cadastro = tela === 'cadastro'
  const perfis = { CLIENTE: 'Cliente', EMPRESA: 'Empresa', BANCO: 'Banco' }

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
        definirSenha('')
        if (dadosUsuario.perfil === 'CLIENTE') await carregarCliente()
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

  function sair() {
    autorizacao.current = null
    definirUsuario(null)
    definirCliente(null)
    definirFormularioCliente(dadosVazios)
    definirNome('')
    definirNomeUsuario('')
    definirSenha('')
    definirMensagem(null)
    definirTela('login')
  }

  return (
    <main className={`pagina ${usuario?.perfil === 'CLIENTE' ? 'pagina-larga' : ''} ${!usuario ? 'pagina-acesso' : ''}`}>
      {!usuario && <aside className="painel-ilustracao" aria-label="Imagem de automóvel"><img src="/carro-estrada.png" alt="Carro em uma estrada cercada por árvores" /></aside>}
      <div className="conteudo-pagina">
      <header className="cabecalho">
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
            ) : <button type="button" onClick={consultarUsuario} disabled={carregando}>Consultar minha conta</button>}
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
