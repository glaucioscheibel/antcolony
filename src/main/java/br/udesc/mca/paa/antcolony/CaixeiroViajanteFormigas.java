package br.udesc.mca.paa.antcolony;

import static java.lang.Math.pow;
import static java.lang.System.arraycopy;
import static java.lang.System.out;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CaixeiroViajanteFormigas {
    private static final int numCidades = 150;
    private static final int numFormigas = 110;
    private static final int viagens = 20;

    private static final double alpha = 3; // peso do feromonio
    private static final double beta = 2; // peso da distancia
    private static final double reducaoFeromonio = 0.01; // 10% de perda de
                                                         // feromonio
    private static final double aumentoFeromonio = 2.0; // 200% de ganho de
                                                        // feromonio
    private static int[][] distancias;
    private static int[][] formigas;
    private static double[][] feromonios;
    private static int[] melhorRota;
    private static int melhorDistancia;
    private static Random random = new Random(System.currentTimeMillis());
    private static Map<Integer, Integer> resultados;

    private CaixeiroViajanteFormigas() {}

    public static void main(String[] args) {
        criaDistancias();
        imprimeTabelaDistancias();
        distribuiFormigas();
        inicializaFeromonios();
        for (int i = 0; i < viagens; i++) {
            out.printf("\nInício da viagem %d\n\n", i);
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
            out.printf("\nFim da viagem %d\n\n", i);
            armazenaResultados(i);
        }
        apresentaGrafico();
    }

    /**
     * Cria randomicamente as distancias entre as cidades
     * 
     * Complexidade: O(n^2)
     */
    private static void criaDistancias() {
        int dist;
        distancias = new int[numCidades][numCidades];
        for (int i = 0; i < numCidades; i++) {
            for (int j = i; j < numCidades; j++) {
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
     * 
     * Complexidade: 2.O(n^2)
     */
    private static void distribuiFormigas() {
        formigas = new int[numFormigas][numCidades + 1];
        for (int i = 0; i < numFormigas; i++) {
            for (int j = 0; j < numCidades; j++) {
                formigas[i][j] = j;
            }
        }
        for (int i = 0; i < numFormigas; i++) {
            for (int j = 0; j < numCidades; j++) {
                int dest = random.nextInt(numCidades);
                if (dest != j) {
                    int aux = formigas[i][j];
                    formigas[i][j] = formigas[i][dest];
                    formigas[i][dest] = aux;
                }
            }
            formigas[i][numCidades] = formigas[i][0]; // volta para o inicio
        }
    }

    /**
     * Calcula a distância de uma rota
     * 
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
     * 
     * Complexidade: O(n)
     */
    private static void melhorRota() {
        for (int i = 0; i < numFormigas; i++) {
            int dist = distanciaPercorrida(formigas[i]);
            if (melhorDistancia == 0 || dist < melhorDistancia) {
                if (melhorRota == null) {
                    melhorRota = new int[numCidades + 1];
                }
                melhorDistancia = dist;
                arraycopy(formigas[i], 0, melhorRota, 0, formigas[i].length);
            }
        }
    }

    /**
     * Inicializa a quantidade de feromonios nos trajetos
     * 
     * Complexidade: O(n^2)
     */
    private static void inicializaFeromonios() {
        feromonios = new double[numCidades][numCidades];
        for (int i = 0; i < numCidades; i++) {
            for (int j = 0; j < numCidades; j++) {
                feromonios[i][j] = 0.01;
            }
        }
    }

    /**
     * Inicia uma nova viagem das formigas
     *
     * Complexidade: O(n) + complexidade da fun��o viagemFormiga
     */
    private static void atualizaRotasFormigas() {
        for (int i = 0; i < numFormigas; i++) {
            // cidade atual da formiga
            // int cidade = formigas[i][numCidades - 1];
            int cidade = random.nextInt(numCidades);
            formigas[i] = viagemFormiga(cidade);
        }
    }

    /**
     * Cria uma rota a partir de uma cidade inicial
     * 
     * Complexidade: O(n) + complexidade da função proximaCidade
     */
    private static int[] viagemFormiga(int inicio) {
        boolean[] visitadas = new boolean[numCidades];
        int[] novaRota = new int[numCidades + 1];
        novaRota[0] = inicio;
        visitadas[inicio] = true;
        for (int i = 0; i < numCidades - 1; i++) {
            int origem = novaRota[i];
            int destino = proximaCidade(origem, visitadas);
            novaRota[i + 1] = destino;
            visitadas[destino] = true;
        }
        novaRota[numCidades] = novaRota[0];
        return novaRota;
    }

    /**
     * Define a proxima cidade a ser visitada levando em consideração os
     * feromonios e a distância do trajeto
     * 
     * Complexidade: O(n^3) + O(n/2)
     */
    private static int proximaCidade(int origem, boolean[] visitadas) {
        double[] aux = new double[numCidades];
        double soma = 0.0;
        for (int i = 0; i < numCidades; i++) {
            if (i != origem && !visitadas[i]) {
                aux[i] = pow(feromonios[origem][i], alpha) * pow(0.1 / distancias[origem][i], beta);
                soma += aux[i];
            }
        }
        for (int i = 0; i < numCidades; i++) {
            aux[i] /= soma;
        }
        double[] acum = new double[numCidades + 1];
        for (int i = 0; i < numCidades; i++) {
            acum[i + 1] = acum[i] + aux[i];
        }
        double p = random.nextDouble();

        for (int i = 0; i < acum.length - 1; ++i) {
            if (p >= acum[i] && p < acum[i + 1]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Atualiza a tabela de feromonios
     * 
     * Complexidade: O(n^2) * O(m) * complexidade da função distanciaPercorrida *
     *               complexidade da função formigaFezCaminho
     */
    private static void atualizaFeromonios() {
        for (int i = 0; i < numCidades; i++) {
            for (int j = 0; j < numCidades; j++) {
                if (j == i) {
                    continue;
                }
                for (int f = 0; f < numFormigas; f++) {
                    int dist = distanciaPercorrida(formigas[f]);
                    double reducao = feromonios[i][j] * (1 - reducaoFeromonio);
                    double aumento = 0.0;
                    if (formigaFezCaminho(f, i, j)) {
                        aumento = aumentoFeromonio / dist;
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
     * Verfica se a formiga fez o trajeto da cidade origem a destino
     * 
     * Complexidade: O(n)
     */
    private static boolean formigaFezCaminho(int formiga, int origem, int destino) {
        if (origem == destino) {
            return false;
        }
        for (int i = 0; i < numCidades + 1; i++) {
            if (origem == formigas[formiga][i]) {
                if (i == 0 && formigas[formiga][i + 1] == destino) {
                    return true;
                } else if (i == numCidades - 1 && formigas[formiga][i - 1] == destino) {
                    return true;
                } else if (i > 0 && i < numCidades - 1) {
                    if (formigas[formiga][i - 1] == destino || formigas[formiga][i + 1] == destino) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Armazena o melhores caminhos para desenho do grafico
     */
    private static void armazenaResultados(int numViagem) {
        if (resultados == null) {
            resultados = new LinkedHashMap<Integer, Integer>();
        }
        resultados.put(numViagem + 1, distanciaPercorrida(melhorRota));
    }

    private static void imprimeTabelaDistancias() {
        out.printf("Distancias:\n");
        out.printf("  |");
        for (int i = 0; i < numCidades; i++) {
            out.printf("%1$2d|", i);
        }
        out.printf("\n--+");
        for (int i = 0; i < numCidades; i++) {
            out.printf("--+", i);
        }
        out.printf("\n");
        for (int i = 0; i < numCidades; i++) {
            out.printf("%1$2d|", i);
            for (int j = 0; j < numCidades; j++) {
                out.printf("%1$2d|", distancias[i][j]);
            }
            out.printf("\n");
        }
        out.printf("--+");
        for (int i = 0; i < numCidades; i++) {
            out.printf("--+", i);
        }
        out.printf("\n");
    }

    private static void imprimeTabelaFeromonios() {
        out.printf("Feromonios:\n");
        out.printf("  |");
        for (int i = 0; i < numCidades; i++) {
            out.printf("%1$6d|", i);
        }
        out.printf("\n--+");
        for (int i = 0; i < numCidades; i++) {
            out.printf("------+", i);
        }
        out.printf("\n");
        for (int i = 0; i < numCidades; i++) {
            out.printf("%1$2d|", i);
            for (int j = 0; j < numCidades; j++) {
                out.printf("%1$1.4f|", feromonios[i][j]);
            }
            out.printf("\n");
        }
        out.printf("--+");
        for (int i = 0; i < numCidades; i++) {
            out.printf("------+", i);
        }
        out.printf("\n");
    }

    private static void imprimeRotas() {
        for (int i = 0; i < numFormigas; i++) {
            out.printf("Formiga %2d fez a rota: %s de distancia %d\n", i, Arrays.toString(formigas[i]),
                    distanciaPercorrida(formigas[i]));
        }
    }

    private static void imprimeMelhorRota() {
        out.printf("melhor rota: %s de distancia %d\n", Arrays.toString(melhorRota), distanciaPercorrida(melhorRota));
    }

    private static void apresentaGrafico() {
        JFrame jf = new JFrame("Grafico das Formigas");
        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        XYSeries xy = new XYSeries("Distancia");
        for (Integer v : resultados.keySet()) {
            xy.add(v, resultados.get(v));
        }
        XYSeriesCollection col = new XYSeriesCollection(xy);
        JFreeChart jfc = ChartFactory.createXYLineChart("ACO", "viagem", "Distância", col, PlotOrientation.VERTICAL,
                true, true, false);
        ChartPanel cp = new ChartPanel(jfc);
        jf.add(cp);
        jf.pack();
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }
}
