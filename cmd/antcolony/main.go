package main

import (
	"fmt"
	"math"
	"math/rand"
	"os"
	"time"

	"github.com/go-echarts/go-echarts/v2/charts"
	"github.com/go-echarts/go-echarts/v2/opts"
)

const (
	NumCidades       = 150
	NumFormigas      = 110
	Viagens          = 20
	Alpha            = 3.0  // peso do feromônio
	Beta             = 2.0  // peso da distância
	ReducaoFeromonio = 0.01 // 1% de perda de feromônio
	AumentoFeromonio = 2.0  // 200% de ganho de feromônio
)

var (
	distancias      [][]int
	formigas        [][]int
	feromonios      [][]float64
	melhorRota      []int
	melhorDistancia int
	rng             *rand.Rand
	resultados      map[int]int
)

func main() {
	rng = rand.New(rand.NewSource(time.Now().UnixNano()))
	resultados = make(map[int]int)

	criaDistancias()
	imprimeTabelaDistancias()
	distribuiFormigas()
	inicializaFeromonios()

	for i := 0; i < Viagens; i++ {
		fmt.Printf("\nInício da viagem %d\n\n", i)
		atualizaFeromonios()
		if i > 0 {
			atualizaRotasFormigas()
		}
		melhorRotaFunc()
		imprimeRotas()
		fmt.Println()
		imprimeMelhorRota()
		fmt.Println()
		imprimeTabelaFeromonios()
		fmt.Printf("\nFim da viagem %d\n\n", i)
		armazenaResultados(i)
	}

	// Gera o gráfico interativo com go-echarts
	geraGraficoEcharts()
}

// Cria randomicamente as distâncias entre as cidades
func criaDistancias() {
	distancias = make([][]int, NumCidades)
	for i := range distancias {
		distancias[i] = make([]int, NumCidades)
	}
	for i := 0; i < NumCidades; i++ {
		for j := i; j < NumCidades; j++ {
			var dist int
			if i == j {
				dist = 0
			} else {
				dist = rng.Intn(20) + 1
			}
			distancias[i][j] = dist
			distancias[j][i] = dist
		}
	}
}

// Cria rotas iniciais aleatórias para cada formiga
func distribuiFormigas() {
	formigas = make([][]int, NumFormigas)
	for i := range formigas {
		formigas[i] = make([]int, NumCidades+1)
		for j := 0; j < NumCidades; j++ {
			formigas[i][j] = j
		}
	}
	for i := 0; i < NumFormigas; i++ {
		for j := 0; j < NumCidades; j++ {
			dest := rng.Intn(NumCidades)
			if dest != j {
				formigas[i][j], formigas[i][dest] = formigas[i][dest], formigas[i][j]
			}
		}
		formigas[i][NumCidades] = formigas[i][0]
	}
}

// Calcula a distância de uma rota
func distanciaPercorrida(cidades []int) int {
	ret := 0
	for i := 0; i < len(cidades)-1; i++ {
		ret += distancias[cidades[i]][cidades[i+1]]
	}
	return ret
}

// Atualiza a melhor rota avaliando a distância percorrida pelas formigas
func melhorRotaFunc() {
	for i := 0; i < NumFormigas; i++ {
		dist := distanciaPercorrida(formigas[i])
		if melhorDistancia == 0 || dist < melhorDistancia {
			if melhorRota == nil {
				melhorRota = make([]int, NumCidades+1)
			}
			melhorDistancia = dist
			copy(melhorRota, formigas[i])
		}
	}
}

// Inicializa a quantidade de feromônios nos trajetos
func inicializaFeromonios() {
	feromonios = make([][]float64, NumCidades)
	for i := range feromonios {
		feromonios[i] = make([]float64, NumCidades)
		for j := 0; j < NumCidades; j++ {
			feromonios[i][j] = 0.01
		}
	}
}

// Inicia uma nova viagem das formigas
func atualizaRotasFormigas() {
	for i := 0; i < NumFormigas; i++ {
		cidade := rng.Intn(NumCidades)
		formigas[i] = viagemFormiga(cidade)
	}
}

// Cria uma rota a partir de uma cidade inicial
func viagemFormiga(inicio int) []int {
	visitadas := make([]bool, NumCidades)
	novaRota := make([]int, NumCidades+1)
	novaRota[0] = inicio
	visitadas[inicio] = true
	for i := 0; i < NumCidades-1; i++ {
		origem := novaRota[i]
		destino := proximaCidade(origem, visitadas)
		novaRota[i+1] = destino
		visitadas[destino] = true
	}
	novaRota[NumCidades] = novaRota[0]
	return novaRota
}

// Define a próxima cidade a ser visitada baseada em feromônios e distâncias
func proximaCidade(origem int, visitadas []bool) int {
	aux := make([]float64, NumCidades)
	soma := 0.0
	for i := 0; i < NumCidades; i++ {
		if i != origem && !visitadas[i] {
			aux[i] = math.Pow(feromonios[origem][i], Alpha) * math.Pow(0.1/float64(distancias[origem][i]), Beta)
			soma += aux[i]
		}
	}
	if soma > 0 {
		for i := 0; i < NumCidades; i++ {
			aux[i] /= soma
		}
	}
	acumulado := make([]float64, NumCidades+1)
	for i := 0; i < NumCidades; i++ {
		acumulado[i+1] = acumulado[i] + aux[i]
	}
	p := rng.Float64()

	for i := 0; i < len(acumulado)-1; i++ {
		if p >= acumulado[i] && p < acumulado[i+1] {
			return i
		}
	}
	return 0
}

// Atualiza a tabela de feromônios
func atualizaFeromonios() {
	for i := 0; i < NumCidades; i++ {
		for j := 0; j < NumCidades; j++ {
			if j == i {
				continue
			}
			for f := 0; f < NumFormigas; f++ {
				dist := distanciaPercorrida(formigas[f])
				reducao := feromonios[i][j] * (1 - ReducaoFeromonio)
				aumento := 0.0
				if formigaFezCaminho(f, i, j) {
					aumento = AumentoFeromonio / float64(dist)
				}
				feromonios[i][j] = reducao + aumento
				if feromonios[i][j] < 0.0001 {
					feromonios[i][j] = 0.0001
				}
			}
		}
	}
}

// Verifica se a formiga fez o trajeto da cidade origem a destino
func formigaFezCaminho(formiga, origem, destino int) bool {
	if origem == destino {
		return false
	}
	for i := 0; i < NumCidades+1; i++ {
		if origem == formigas[formiga][i] {
			if i == 0 && formigas[formiga][i+1] == destino {
				return true
			} else if i == NumCidades-1 && formigas[formiga][i-1] == destino {
				return true
			} else if i > 0 && i < NumCidades-1 {
				return formigas[formiga][i-1] == destino || formigas[formiga][i+1] == destino
			}
			return false
		}
	}
	return false
}

// Armazena o melhor caminho por viagem
func armazenaResultados(numViagem int) {
	resultados[numViagem+1] = distanciaPercorrida(melhorRota)
}

// Gera o gráfico utilizando go-echarts/v2 salvando em um arquivo HTML
func geraGraficoEcharts() {
	line := charts.NewLine()
	line.SetGlobalOptions(
		charts.WithTitleOpts(opts.Title{
			Title:    "Algoritmo de Colônia de Formigas (ACO)",
			Subtitle: "Evolução da menor distância por viagem",
		}),
		charts.WithToolboxOpts(opts.Toolbox{
			Show: opts.Bool(true),
		}),
	)

	xValues := make([]string, Viagens)
	yValues := make([]opts.LineData, Viagens)

	for i := 1; i <= Viagens; i++ {
		xValues[i-1] = fmt.Sprintf("Viagem %d", i)
		yValues[i-1] = opts.LineData{Value: resultados[i]}
	}

	line.SetXAxis(xValues).
		AddSeries("Menor Distância", yValues).
		SetSeriesOptions(charts.WithLineChartOpts(opts.LineChart{
			Smooth: opts.Bool(true),
		}))

	f, err := os.Create("aco_grafico.html")
	if err != nil {
		fmt.Println("Erro ao criar arquivo HTML do gráfico:", err)
		return
	}
	defer f.Close()

	_ = line.Render(f)
	fmt.Println("\nGráfico gerado com sucesso! Abra o arquivo 'aco_grafico.html' no seu navegador.")
}

func imprimeTabelaDistancias() {
	fmt.Println("Distâncias:")
	fmt.Print("  |")
	for i := 0; i < NumCidades; i++ {
		fmt.Printf("%2d|", i)
	}
	fmt.Printf("\n--+")
	for i := 0; i < NumCidades; i++ {
		fmt.Print("--+")
	}
	fmt.Println()
	for i := 0; i < NumCidades; i++ {
		fmt.Printf("%2d|", i)
		for j := 0; j < NumCidades; j++ {
			fmt.Printf("%2d|", distancias[i][j])
		}
		fmt.Println()
	}
	fmt.Print("--+")
	for i := 0; i < NumCidades; i++ {
		fmt.Print("--+")
	}
	fmt.Println()
}

func imprimeTabelaFeromonios() {
	fmt.Println("Feromônios:")
	fmt.Print("   |")
	for i := 0; i < NumCidades; i++ {
		fmt.Printf("%6d|", i)
	}
	fmt.Printf("\n---+")
	for i := 0; i < NumCidades; i++ {
		fmt.Print("------+")
	}
	fmt.Println()
	for i := 0; i < NumCidades; i++ {
		fmt.Printf("%3d|", i)
		for j := 0; j < NumCidades; j++ {
			fmt.Printf("%1.4f|", feromonios[i][j])
		}
		fmt.Println()
	}
	fmt.Print("---+")
	for i := 0; i < NumCidades; i++ {
		fmt.Print("------+")
	}
	fmt.Println()
}

func imprimeRotas() {
	for i := 0; i < NumFormigas; i++ {
		fmt.Printf("Formiga %3d fez a rota: %v de distancia %d\n", i, formigas[i], distanciaPercorrida(formigas[i]))
	}
}

func imprimeMelhorRota() {
	fmt.Printf("melhor rota: %v de distância %d\n", melhorRota, distanciaPercorrida(melhorRota))
}
