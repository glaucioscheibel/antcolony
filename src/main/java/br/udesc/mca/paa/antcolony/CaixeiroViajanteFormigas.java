package br.udesc.mca.paa.antcolony;

import static java.lang.Math.pow;
import static java.lang.System.arraycopy;
import static java.lang.System.out;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CaixeiroViajanteFormigas {
    private static final int NUM_CIDADES = 150;
    private static final int NUM_FORMIGAS = 110;
    private static final int VIAGENS = 20;

    private static final double ALPHA = 3; // peso do feromonio
    private static final double BETA = 2; // peso da distancia
    private static final double REDUCAO_FEROMONIO = 0.01; // 10% de perda de feromonio
    private static final double AUMENTO_FEROMONIO = 2.0; // 200% de ganho de feromonio
    private static int[][] distancias;
    private static int[][] formigas;
    private static double[][] feromonios;
    private static int[] melhorRota;
    private static int melhorDistancia;
    private static final Random random = new Random(System.currentTimeMillis());
    private static Map<Integer, Integer> resultados;

    private CaixeiroViajanteFormigas() {}

    static void main() {
        criaDistancias();
        imprimeTabelaDistancias();
        distribuiFormigas();
        inicializaFeromonios();
        for (int i = 0; i < VIAGENS; i++) {
            out.printf("%nInício da viagem %d%n%n", i);
            atualizaFeromonios();
            if (i > 0) {
                atualizaRotasFormigas();
            }
            melhorRota();
            imprimeRotas();
            out.println();
            imprimeMelhorRota();
            out.println();
            imprimeTabelaFeromonios();
            out.printf("%nFim da viagem %d%n%n", i);
            armazenaResultados(i);
        }
        apresentaGrafico();
    }

    /**
     * Cria randomicamente as distâncias entre as cidades
     * Complexidade: O(n^2)
     */
    private static void criaDistancias() {
        int dist;
        distancias = new int[NUM_CIDADES][NUM_CIDADES];
        for (int i = 0; i < NUM_CIDADES; i++) {
            for (int j = i; j < NUM_CIDADES; j++) {
                if (i == j) {
                    dist = 0;
                } else {
                    dist = random.nextInt(20) + 1;
                }
                distancias[i][j] = dist;
                distancias[j][i] = dist;
            }
        }
    }

    /**
     * Cria rotas iniciais aleatórias para cada formiga
     * Complexidade: 2*O(n^2)
     */
    private static void distribuiFormigas() {
        formigas = new int[NUM_FORMIGAS][NUM_CIDADES + 1];
        for (int i = 0; i < NUM_FORMIGAS; i++) {
            for (int j = 0; j < NUM_CIDADES; j++) {
                formigas[i][j] = j;
            }
        }
        for (int i = 0; i < NUM_FORMIGAS; i++) {
            for (int j = 0; j < NUM_CIDADES; j++) {
                int dest = random.nextInt(NUM_CIDADES);
                if (dest != j) {
                    int aux = formigas[i][j];
                    formigas[i][j] = formigas[i][dest];
                    formigas[i][dest] = aux;
                }
            }
            formigas[i][NUM_CIDADES] = formigas[i][0]; // volta para o início
        }
    }

    /**
     * Calcula a distância de uma rota
     * Complexidade: O(n)
     */
    private static int distanciaPercorrida(int[] cidades) {
        int ret = 0;
        for (int i = 0; i < cidades.length - 1; i++) {
            ret += distancias[cidades[i]][cidades[i + 1]];
        }
        return ret;
    }

    /**
     * Atualiza a melhor rota avaliando a distância percorrida pelas formigas
     * Complexidade: O(n)
     */
    private static void melhorRota() {
        for (int i = 0; i < NUM_FORMIGAS; i++) {
            int dist = distanciaPercorrida(formigas[i]);
            if (melhorDistancia == 0 || dist < melhorDistancia) {
                if (melhorRota == null) {
                    melhorRota = new int[NUM_CIDADES + 1];
                }
                melhorDistancia = dist;
                arraycopy(formigas[i], 0, melhorRota, 0, formigas[i].length);
            }
        }
    }

    /**
     * Inicializa a quantidade de feromonios nos trajetos
     * Complexidade: O(n^2)
     */
    private static void inicializaFeromonios() {
        feromonios = new double[NUM_CIDADES][NUM_CIDADES];
        for (int i = 0; i < NUM_CIDADES; i++) {
            for (int j = 0; j < NUM_CIDADES; j++) {
                feromonios[i][j] = 0.01;
            }
        }
    }

    /**
     * Inicia uma nova viagem das formigas
     * Complexidade: O(n) + complexidade da função viagemFormiga
     */
    private static void atualizaRotasFormigas() {
        for (int i = 0; i < NUM_FORMIGAS; i++) {
            int cidade = random.nextInt(NUM_CIDADES);
            formigas[i] = viagemFormiga(cidade);
        }
    }

    /**
     * Cria uma rota a partir de uma cidade inicial
     * Complexidade: O(n) + complexidade da função proximaCidade
     */
    private static int[] viagemFormiga(int inicio) {
        boolean[] visitadas = new boolean[NUM_CIDADES];
        int[] novaRota = new int[NUM_CIDADES + 1];
        novaRota[0] = inicio;
        visitadas[inicio] = true;
        for (int i = 0; i < NUM_CIDADES - 1; i++) {
            int origem = novaRota[i];
            int destino = proximaCidade(origem, visitadas);
            novaRota[i + 1] = destino;
            visitadas[destino] = true;
        }
        novaRota[NUM_CIDADES] = novaRota[0];
        return novaRota;
    }

    /**
     * Define a proxima cidade a ser visitada levando em consideração os
     * feromonios e a distância do trajeto
     * Complexidade: O(n^3) + O(n/2)
     */
    private static int proximaCidade(int origem, boolean[] visitadas) {
        double[] aux = new double[NUM_CIDADES];
        double soma = 0.0;
        for (int i = 0; i < NUM_CIDADES; i++) {
            if (i != origem && !visitadas[i]) {
                aux[i] = pow(feromonios[origem][i], ALPHA) * pow(0.1 / distancias[origem][i], BETA);
                soma += aux[i];
            }
        }
        for (int i = 0; i < NUM_CIDADES; i++) {
            aux[i] /= soma;
        }
        double[] acumulado = new double[NUM_CIDADES + 1];
        for (int i = 0; i < NUM_CIDADES; i++) {
            acumulado[i + 1] = acumulado[i] + aux[i];
        }
        double p = random.nextDouble();

        for (int i = 0; i < acumulado.length - 1; ++i) {
            if (p >= acumulado[i] && p < acumulado[i + 1]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Atualiza a tabela de feromônios
     * Complexidade: O(n^2) * O(m) * complexidade da função distanciaPercorrida *
     *               complexidade da função formigaFezCaminho
     */
    private static void atualizaFeromonios() {
        for (int i = 0; i < NUM_CIDADES; i++) {
            for (int j = 0; j < NUM_CIDADES; j++) {
                if (j == i) {
                    continue;
                }
                for (int f = 0; f < NUM_FORMIGAS; f++) {
                    int dist = distanciaPercorrida(formigas[f]);
                    double reducao = feromonios[i][j] * (1 - REDUCAO_FEROMONIO);
                    double aumento = 0.0;
                    if (formigaFezCaminho(f, i, j)) {
                        aumento = AUMENTO_FEROMONIO / dist;
                    }
                    feromonios[i][j] = reducao + aumento;
                    if (feromonios[i][j] < 0.0001) {
                        feromonios[i][j] = 0.0001;
                    }
                }
            }
        }
    }

    /**
     * Verifica se a formiga fez o trajeto da cidade origem a destino
     * Complexidade: O(n)
     */
    private static boolean formigaFezCaminho(int formiga, int origem, int destino) {
        if (origem == destino) {
            return false;
        }
        for (int i = 0; i < NUM_CIDADES + 1; i++) {
            if (origem == formigas[formiga][i]) {
                if (i == 0 && formigas[formiga][i + 1] == destino) {
                    return true;
                } else if (i == NUM_CIDADES - 1 && formigas[formiga][i - 1] == destino) {
                    return true;
                } else if (i > 0 && i < NUM_CIDADES - 1) {
                    return formigas[formiga][i - 1] == destino || formigas[formiga][i + 1] == destino;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Armazena o melhor caminho para desenho do gráfico
     */
    private static void armazenaResultados(int numViagem) {
        if (resultados == null) {
            resultados = new LinkedHashMap<>();
        }
        resultados.put(numViagem + 1, distanciaPercorrida(melhorRota));
    }

    private static void imprimeTabelaDistancias() {
        out.println("Distâncias:");
        out.print("  |");
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.printf("%1$2d|", i);
        }
        out.printf("%n--+");
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.print("--+");
        }
        out.println();
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.printf("%1$2d|", i);
            for (int j = 0; j < NUM_CIDADES; j++) {
                out.printf("%1$2d|", distancias[i][j]);
            }
            out.println();
        }
        out.print("--+");
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.print("--+");
        }
        out.println();
    }

    private static void imprimeTabelaFeromonios() {
        out.println("Feromônios:");
        out.print("   |");
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.printf("%1$6d|", i);
        }
        out.printf("%n---+");
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.print("------+");
        }
        out.println();
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.printf("%1$3d|", i);
            for (int j = 0; j < NUM_CIDADES; j++) {
                out.printf("%1$1.4f|", feromonios[i][j]);
            }
            out.println();
        }
        out.print("---+");
        for (int i = 0; i < NUM_CIDADES; i++) {
            out.print("------+");
        }
        out.println();
    }

    private static void imprimeRotas() {
        for (int i = 0; i < NUM_FORMIGAS; i++) {
            out.printf(
                    "Formiga %3d fez a rota: %s de distancia %d%n",
                    i, Arrays.toString(formigas[i]), distanciaPercorrida(formigas[i]));
        }
    }

    private static void imprimeMelhorRota() {
        out.printf("melhor rota: %s de distância %d%n", Arrays.toString(melhorRota), distanciaPercorrida(melhorRota));
    }

    private static void apresentaGrafico() {
        JFrame jf = new JFrame("Gráfico das Formigas");
        jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        XYSeries xy = new XYSeries("Distância");
        for (Map.Entry<Integer, Integer> entry : resultados.entrySet()) {
            xy.add(entry.getKey(), entry.getValue());
        }
        XYSeriesCollection col = new XYSeriesCollection(xy);
        JFreeChart jfc = ChartFactory.createXYLineChart(
                "ACO", "viagem", "Distância", col, PlotOrientation.VERTICAL, true, true, false);
        ChartPanel cp = new ChartPanel(jfc);
        jf.add(cp);
        jf.pack();
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }
}
