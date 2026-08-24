import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Aplicacao SERVIDOR.
 *
 * Recebe requisicoes do Cliente via socket TCP. Para cada requisicao abre
 * duas threads:
 *   - Thread A: le o conteudo do arquivo A.txt e conta os caracteres.
 *   - Thread B: escreve no arquivo B.txt o numero do processo (PID), a data
 *               e a hora da requisicao.
 *
 * Se as duas acoes ocorrerem sem problemas, devolve ao Cliente uma mensagem
 * de confirmacao contendo a quantidade de caracteres do A.txt.
 */
public class Servidor {

    private static final int PORTA = 5000;
    private static final String ARQUIVO_A = "A.txt";
    private static final String ARQUIVO_B = "B.txt";

    // Lock para que requisicoes concorrentes nao embaralhem as escritas no B.txt.
    private static final ReentrantLock LOCK_B = new ReentrantLock();

    /** Thread que le o A.txt e guarda a quantidade de caracteres. */
    static class LeitorA extends Thread {
        private long qtdCaracteres = -1;
        private Exception erro;

        @Override
        public void run() {
            try {
                String conteudo = new String(
                        Files.readAllBytes(Paths.get(ARQUIVO_A)), StandardCharsets.UTF_8);
                qtdCaracteres = conteudo.length();
            } catch (Exception e) {
                erro = e;
            }
        }

        long getQtdCaracteres() {
            return qtdCaracteres;
        }

        Exception getErro() {
            return erro;
        }
    }

    /** Thread que escreve PID, data e hora da requisicao no B.txt. */
    static class EscritorB extends Thread {
        private final String enderecoCliente;
        private Exception erro;

        EscritorB(String enderecoCliente) {
            this.enderecoCliente = enderecoCliente;
        }

        @Override
        public void run() {
            String pid = String.valueOf(ProcessHandle.current().pid());
            String agora = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            String linha = "Processo: " + pid + " | Data/Hora: " + agora
                    + " | Cliente: " + enderecoCliente + System.lineSeparator();

            // O lock evita que escritas concorrentes se misturem.
            LOCK_B.lock();
            try (PrintWriter escritor = new PrintWriter(
                    Files.newBufferedWriter(Paths.get(ARQUIVO_B),
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND))) {
                escritor.print(linha);
            } catch (Exception e) {
                erro = e;
            } finally {
                LOCK_B.unlock();
            }
        }

        Exception getErro() {
            return erro;
        }
    }

    private static void tratarRequisicao(Socket conexao) {
        String endereco = conexao.getInetAddress().getHostAddress() + ":" + conexao.getPort();
        try (Socket s = conexao) {
            Scanner entrada = new Scanner(s.getInputStream(), StandardCharsets.UTF_8);
            PrintWriter saida = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);

            String requisicao = entrada.hasNextLine() ? entrada.nextLine() : "";
            System.out.println("[SERVIDOR] Requisicao recebida de " + endereco + ": " + requisicao);

            LeitorA leitor = new LeitorA();
            EscritorB escritor = new EscritorB(endereco);
            leitor.start();
            escritor.start();

            leitor.join();
            escritor.join();

            String resposta;
            if (leitor.getErro() != null) {
                resposta = "ERRO ao ler " + ARQUIVO_A + ": " + leitor.getErro();
            } else if (escritor.getErro() != null) {
                resposta = "ERRO ao escrever " + ARQUIVO_B + ": " + escritor.getErro();
            } else {
                resposta = "Confirmacao: requisicao processada com sucesso. "
                        + "O arquivo " + ARQUIVO_A + " possui " + leitor.getQtdCaracteres()
                        + " caracteres.";
            }

            saida.println(resposta);
            System.out.println("[SERVIDOR] Resposta enviada para " + endereco + ": " + resposta);
        } catch (IOException | InterruptedException e) {
            System.out.println("[SERVIDOR] Erro ao tratar " + endereco + ": " + e);
        }
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("[SERVIDOR] Escutando na porta " + PORTA
                    + " (PID " + ProcessHandle.current().pid() + ")");
            System.out.println("[SERVIDOR] Pressione Ctrl+C para encerrar.");

            while (true) {
                Socket conexao = servidor.accept();
                // Uma thread por conexao => atende varios clientes ao mesmo tempo.
                new Thread(() -> tratarRequisicao(conexao)).start();
            }
        }
    }
}
