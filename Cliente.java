import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Aplicacao CLIENTE.
 *
 * Conecta-se ao Servidor via socket TCP, envia uma requisicao e apresenta
 * em tela a mensagem de confirmacao retornada (que contem a quantidade de
 * caracteres do arquivo A.txt).
 */
public class Cliente {

    private static final String HOST = "127.0.0.1";
    private static final int PORTA = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORTA)) {
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            Scanner entrada = new Scanner(socket.getInputStream(), StandardCharsets.UTF_8);

            String requisicao = "Solicitacao de processamento";
            saida.println(requisicao);
            System.out.println("[CLIENTE] Requisicao enviada: " + requisicao);

            String resposta = entrada.hasNextLine() ? entrada.nextLine() : "";
            System.out.println("[CLIENTE] Resposta do servidor: " + resposta);
        } catch (IOException e) {
            System.out.println("[CLIENTE] Erro ao conectar ao servidor: " + e);
        }
    }
}
