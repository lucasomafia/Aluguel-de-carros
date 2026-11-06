import { useRef, useState } from 'react'

async function chamarApi(caminho, metodo, autorizacao, dados) {
  let resposta

  try {
    resposta = await fetch(`/api/auth/${caminho}`, {
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

  const resultado = await resposta.json().catch(() => null)

  if (!resposta.ok) {
    if (resposta.status === 401) {
      throw new Error('Usuário ou senha incorretos.')
    }
    if (resposta.status === 409) {
      throw new Error('Este nome de usuário já está cadastrado.')
    }
    if (resposta.status >= 500) {
      throw new Error('O sistema está indisponível no momento. Tente novamente mais tarde.')
    }
    throw new Error(resultado?.campos?.join(' • ') || resultado?.detail || 'Não foi possível concluir a operação.')
  }

  if (!resultado?.id || !resultado?.nomeUsuario) {
    throw new Error('Não foi possível concluir a operação. Tente novamente.')
  }

  return resultado
}

function App() {
  const [tela, definirTela] = useState('login')
  const [nome, definirNome] = useState('')
  const [nomeUsuario, definirNomeUsuario] = useState('')
  const [senha, definirSenha] = useState('')
  const [usuario, definirUsuario] = useState(null)
  const [carregando, definirCarregando] = useState(false)
  const [mensagem, definirMensagem] = useState(null)
  const autorizacao = useRef(null)
  const cadastro = tela === 'cadastro'
  const perfis = { CLIENTE: 'Cliente', EMPRESA: 'Empresa', BANCO: 'Banco' }

  function trocarTela(novaTela) {
    definirTela(novaTela)
    definirMensagem(null)
    definirSenha('')
  }

  async function enviarFormulario(evento) {
    evento.preventDefault()
    definirMensagem(null)

    if (cadastro && new TextEncoder().encode(senha).length > 72) {
      definirMensagem({ erro: true, texto: 'A senha deve ocupar no máximo 72 bytes. Use menos caracteres acentuados ou emojis.' })
      return
    }

    definirCarregando(true)
    try {
      if (cadastro) {
        await chamarApi('cadastro', 'POST', null, { nome, nomeUsuario, senha })
        definirTela('login')
        definirNome('')
        definirSenha('')
        definirMensagem({ erro: false, texto: 'Conta criada! Entre com seu usuário e senha.' })
      } else {
        const bytes = new TextEncoder().encode(`${nomeUsuario}:${senha}`)
        const credencial = `Basic ${btoa(String.fromCharCode(...bytes))}`
        const dadosUsuario = await chamarApi('login', 'POST', credencial)
        autorizacao.current = credencial
        definirUsuario(dadosUsuario)
        definirSenha('')
      }
    } catch (erro) {
      definirMensagem({ erro: true, texto: erro.message })
    } finally {
      definirCarregando(false)
    }
  }

  async function consultarUsuario() {
    definirCarregando(true)
    definirMensagem(null)
    try {
      definirUsuario(await chamarApi('me', 'GET', autorizacao.current))
      definirMensagem({ erro: false, texto: 'Dados atualizados com sucesso.' })
    } catch (erro) {
      definirMensagem({ erro: true, texto: erro.message })
    } finally {
      definirCarregando(false)
    }
  }

  function sair() {
    autorizacao.current = null
    definirUsuario(null)
    definirNome('')
    definirNomeUsuario('')
    definirSenha('')
    definirMensagem(null)
    definirTela('login')
  }

  return (
    <main className="pagina">
      <header className="cabecalho">
        <h1>Aluguel de carros</h1>
        <p>Cadastro e acesso ao sistema</p>
      </header>

      <section className="cartao" aria-labelledby="titulo">
        {usuario ? (
          <>
            <h2 id="titulo">Olá, {usuario.nome}!</h2>
            <p className="descricao">Você está autenticado no sistema.</p>
            <dl>
              <div><dt>Nome</dt><dd>{usuario.nome}</dd></div>
              <div><dt>Usuário</dt><dd>{usuario.nomeUsuario}</dd></div>
              <div><dt>Perfil</dt><dd>{perfis[usuario.perfil] || usuario.perfil}</dd></div>
              <div><dt>Identificador</dt><dd>{usuario.id}</dd></div>
            </dl>
          </>
        ) : (
          <>
            <nav className="opcoes" aria-label="Acesso">
              <button type="button" className={cadastro ? '' : 'selecionado'} aria-pressed={!cadastro} onClick={() => trocarTela('login')} disabled={carregando}>Entrar</button>
              <button type="button" className={cadastro ? 'selecionado' : ''} aria-pressed={cadastro} onClick={() => trocarTela('cadastro')} disabled={carregando}>Criar conta</button>
            </nav>
            <h2 id="titulo">{cadastro ? 'Crie sua conta' : 'Entre na sua conta'}</h2>
            <p className="descricao">{cadastro ? 'Cadastre-se como cliente para acessar o sistema.' : 'Informe seu usuário e senha para continuar.'}</p>
          </>
        )}

        {mensagem && <p className={`mensagem ${mensagem.erro ? 'erro' : 'sucesso'}`} role={mensagem.erro ? 'alert' : 'status'}>{mensagem.texto}</p>}

        {usuario ? (
          <div className="acoes">
            <button type="button" onClick={consultarUsuario} disabled={carregando}>{carregando ? 'Consultando...' : 'Consultar meus dados'}</button>
            <button type="button" className="secundario" onClick={sair} disabled={carregando}>Sair</button>
          </div>
        ) : (
          <form onSubmit={enviarFormulario}>
            <fieldset disabled={carregando}>
              {cadastro && (
                <label htmlFor="nome">Nome completo
                  <input id="nome" name="nome" autoComplete="name" value={nome} onChange={(evento) => definirNome(evento.target.value)} required maxLength={120} />
                </label>
              )}
              <label htmlFor="nomeUsuario">Usuário
                <input id="nomeUsuario" name="nomeUsuario" autoComplete="username" autoCapitalize="none" spellCheck={false} value={nomeUsuario} onChange={(evento) => definirNomeUsuario(evento.target.value)} required minLength={3} maxLength={60} pattern={"[a-zA-Z0-9._\\-]{3,60}"} aria-describedby={cadastro ? 'ajudaUsuario' : undefined} />
              </label>
              {cadastro && <small id="ajudaUsuario">Use letras sem acentos, números, ponto, hífen ou sublinhado.</small>}
              <label htmlFor="senha">Senha
                <input id="senha" name="senha" type="password" autoComplete={cadastro ? 'new-password' : 'current-password'} value={senha} onChange={(evento) => definirSenha(evento.target.value)} required minLength={cadastro ? 8 : undefined} maxLength={64} aria-describedby={cadastro ? 'ajudaSenha' : undefined} />
              </label>
              {cadastro && <small id="ajudaSenha">De 8 a 64 caracteres, com no máximo 72 bytes.</small>}
              <button className="enviar" type="submit">{carregando ? 'Aguarde...' : cadastro ? 'Criar conta' : 'Entrar'}</button>
            </fieldset>
          </form>
        )}
      </section>
    </main>
  )
}

export default App
