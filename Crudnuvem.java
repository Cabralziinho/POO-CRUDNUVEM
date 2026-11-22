
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class CrudNuvem {
    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        String usuario = System.getenv("DB_USER");
        String senha = System.getenv("DB_PASSWORD");

        if (vazio(url) || vazio(usuario) || vazio(senha)) {
            System.out.println("Configure DB_URL, DB_USER e DB_PASSWORD no Eclipse.");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver MySQL nao encontrado. Adicione o Connector/J ao Classpath.");
            return;
        }

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha);
             Scanner sc = new Scanner(System.in)) {
            criarTabela(conexao);
            System.out.println("Conectado ao banco na nuvem!");
            int opcao;

            do {
                System.out.println("\n===== CRUD DE PRODUTOS =====");
                System.out.println("1 - Inserir");
                System.out.println("2 - Consultar por codigo");
                System.out.println("3 - Atualizar preco");
                System.out.println("4 - Deletar");
                System.out.println("0 - Sair");
                opcao = lerInteiro(sc, "Escolha uma opcao: ");

                try {
                    switch (opcao) {
                        case 1: inserir(conexao, sc); break;
                        case 2: consultar(conexao, sc); break;
                        case 3: atualizar(conexao, sc); break;
                        case 4: deletar(conexao, sc); break;
                        case 0: System.out.println("Saindo do sistema..."); break;
                        default: System.out.println("Opcao invalida!");
                    }
                } catch (SQLException e) {
                    System.out.println("Erro ao executar operacao no banco: " + e.getMessage());
                }
            } while (opcao != 0);
        } catch (SQLException e) {
            System.out.println("Erro na conexao ou inicializacao do banco: " + e.getMessage());
        }
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private static void criarTabela(Connection conexao) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS PRODUTO ("
                + "CODIGO INT PRIMARY KEY, "
                + "NOME VARCHAR(100) NOT NULL, "
                + "PRECO DECIMAL(10,2) NOT NULL)";
        try (Statement stmt = conexao.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void inserir(Connection conexao, Scanner sc) throws SQLException {
        int codigo = lerInteiro(sc, "Codigo do produto: ");
        String nome;
        do {
            System.out.print("Nome do produto: ");
            nome = sc.nextLine().trim();
            if (nome.isEmpty() || nome.length() > 100)
                System.out.println("Informe um nome entre 1 e 100 caracteres.");
        } while (nome.isEmpty() || nome.length() > 100);

        BigDecimal preco = lerPreco(sc, "Preco do produto: ");
        String sql = "INSERT INTO PRODUTO (CODIGO, NOME, PRECO) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, codigo);
            stmt.setString(2, nome);
            stmt.setBigDecimal(3, preco);
            stmt.executeUpdate();
            System.out.println("Produto inserido com sucesso!");
        }
    }

    private static void consultar(Connection conexao, Scanner sc) throws SQLException {
        int codigo = lerInteiro(sc, "Digite o codigo do produto: ");
        String sql = "SELECT CODIGO, NOME, PRECO FROM PRODUTO WHERE CODIGO = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, codigo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n===== DADOS DO PRODUTO =====");
                    System.out.println("Codigo: " + rs.getInt("CODIGO"));
                    System.out.println("Nome: " + rs.getString("NOME"));
                    System.out.println("Preco: " + rs.getBigDecimal("PRECO"));
                } else {
                    System.out.println("Produto nao localizado!");
                }
            }
        }
    }

    private static void atualizar(Connection conexao, Scanner sc) throws SQLException {
        int codigo = lerInteiro(sc, "Codigo do produto para atualizar: ");
        BigDecimal preco = lerPreco(sc, "Digite o novo preco: ");
        String sql = "UPDATE PRODUTO SET PRECO = ? WHERE CODIGO = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setBigDecimal(1, preco);
            stmt.setInt(2, codigo);
            int alterados = stmt.executeUpdate();
            if (alterados > 0)
                System.out.println("Preco atualizado com sucesso!");
            else
                System.out.println("Produto nao localizado ou preco inalterado!");
        }
    }

    private static void deletar(Connection conexao, Scanner sc) throws SQLException {
        int codigo = lerInteiro(sc, "Codigo do produto para deletar: ");
        String sql = "DELETE FROM PRODUTO WHERE CODIGO = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, codigo);
            int deletados = stmt.executeUpdate();
            if (deletados > 0)
                System.out.println("Produto deletado com sucesso!");
            else
                System.out.println("Produto nao localizado!");
        }
    }

    private static int lerInteiro(Scanner sc, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = sc.nextLine().trim();
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("Digite um numero inteiro valido.");
            }
        }
    }

    private static BigDecimal lerPreco(Scanner sc, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = sc.nextLine().trim().replace(',', '.');
            try {
                BigDecimal preco = new BigDecimal(entrada);
                if (preco.signum() < 0 || preco.scale() > 2
                        || preco.compareTo(new BigDecimal("100000000")) >= 0) {
                    System.out.println("Informe um preco entre 0 e 99999999,99, com ate 2 casas decimais.");
                    continue;
                }
                return preco;
            } catch (NumberFormatException e) {
                System.out.println("Digite um preco valido, como 19,90.");
            }
        }
    }
}
