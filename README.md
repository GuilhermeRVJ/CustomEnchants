# CustomEnchants (Spigot 1.8.8)

Plugin com encantamentos personalizados. Como a 1.8 não tem API de encantamentos custom, ficam guardados no lore do item (ex.: `Telepatia I`).

## Compilar
Precisas de JDK 8+ e Maven:

    mvn clean package

O jar fica em `target/CustomEnchants-1.0.0.jar`. Coloca-o na pasta `plugins/` do servidor (Spigot/PaperSpigot 1.8.8).

## Encantamentos
| Nome | Chave | Max | Ferramentas | O que faz |
|---|---|---|---|---|
| Telepatia | telepathy | I | todas | Drops e XP de blocos e mobs vão direto para o inventário |
| Plantação Automática | autoplant | III | enxada | Colhe culturas maduras e replanta sozinha (usa uma semente dos drops ou do inventário). Nível II = área 3x3, III = 5x5 |
| Fundição | smelting | I | picareta, pá | Drops saem fundidos (usa as receitas de fornalha do servidor) |
| Lenhador | timber | III | machado | Parte a árvore toda (40 blocos por nível) |
| Mineiro de Veios | veinminer | III | picareta | Minera o veio inteiro (8 blocos por nível) |
| Experiência | experience | III | picareta, machado, pá, espada | +50% XP por nível |
| Vampirismo | lifesteal | III | espada | Recupera 10% do dano causado por nível |
| Pressa | haste | III | picareta, machado, pá | Haste enquanto seguras a ferramenta |

## Comandos (`/ce`, permissão `customenchants.admin`)
- `/ce list` – lista os encantamentos (todos os jogadores)
- `/ce enchant <nome> [nível]` – encanta o item na mão
- `/ce remove <nome>` – remove do item na mão
- `/ce book <nome> [nível] [jogador]` – dá um livro; arrasta-o para cima de um item no inventário para aplicar
- `/ce reload` – recarrega o config

## Notas técnicas
- Telepatia/Fundição/etc. cancelam o `BlockBreakEvent` e partem o bloco manualmente (a 1.8 não tem `BlockDropItemEvent`). Os blocos extra (árvore, veio, área) lançam um `BlockBreakEvent` próprio, por isso plugins de proteção (WorldGuard, etc.) são respeitados.
- `Block#getDrops` da 1.8 ignora Fortuna/Toque de Seda e sementes extra, por isso o plugin calcula esses casos (`BlockUtil`).
- Baús, fornos, spawners, cabeças, camas e plantas altas são deixados ao servidor para não perder conteúdo.
- Opções em `config.yml` (limites, sneak obrigatório, mensagens).
