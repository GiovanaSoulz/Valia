package br.com.fiap.trabalhojusto.regras;

import java.time.LocalDate;
import java.util.List;

/**
 * Catálogo versionado das regras. Para refletir uma mudança de lei (ex.: novo salário mínimo),
 * basta adicionar uma nova versão com a data de vigência: os relatórios antigos continuam
 * reproduzíveis porque cada cálculo registra a versão usada.
 *
 * Base legal de referência: CF/88 art. 7º; CLT arts. 58, 59, 66 e 71; LC 150/2015 (doméstico).
 * Os valores devem ser revisados por profissional jurídico antes de uso real.
 */
public final class CatalogoRegras {

    private static final List<VersaoRegras> VERSOES = List.of(
            VersaoRegras.padrao("2024.1", LocalDate.of(2024, 1, 1), "1412.00"),
            VersaoRegras.padrao("2025.1", LocalDate.of(2025, 1, 1), "1518.00"),
            VersaoRegras.padrao("2026.1", LocalDate.of(2026, 1, 1), "1621.00"));

    private CatalogoRegras() {
    }

    /** Versão vigente na data; para datas anteriores ao catálogo usa a mais antiga. */
    public static VersaoRegras vigenteEm(LocalDate data) {
        VersaoRegras escolhida = VERSOES.get(0);
        for (VersaoRegras v : VERSOES) {
            if (!v.vigenciaInicio().isAfter(data)) {
                escolhida = v;
            }
        }
        return escolhida;
    }

    public static List<VersaoRegras> todas() {
        return VERSOES;
    }
}
