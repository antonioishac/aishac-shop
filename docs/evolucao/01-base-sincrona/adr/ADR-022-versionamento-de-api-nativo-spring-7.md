# ADR-022 — Versionamento de API nativo do Spring 7, por segmento de caminho

| Campo | Valor |
|---|---|
| Status | Aceita |
| Data | 2026-10-12 |
| Fase | 01-base-sincrona |
| Origem | Princípio "contratos versionados (`/api/v1`)" · F01-012 |

## Contexto
Os contratos são versionados em `/api/v1`. A forma tradicional é fixar o prefixo no `@RequestMapping` e criar controllers novos para a v2. O Spring Framework 7 trouxe versionamento de API nativo: a versão é resolvida da requisição (caminho, cabeçalho, parâmetro ou *media type*) e cada mapeamento declara a versão que atende.

## Decisão
Usar o **versionamento nativo** com a versão no **segundo segmento do caminho**:
- configuração comum no `config-repo`: `spring.mvc.apiversion.use.path-segment=1` e `spring.mvc.apiversion.supported=1`;
- os controllers mapeiam `@RequestMapping("/api/{version}/...")` e declaram a versão em cada método (`@GetMapping(version = "1")`);
- versão não suportada responde **400 em ProblemDetail**;
- Actuator e `/v3/api-docs` continuam funcionando sem segmento de versão (verificado no `ServiceSkeletonIT`).

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Prefixo fixo `/api/v1` no mapeamento | Óbvio; sem configuração | v2 exige duplicar controllers; versões suportadas não ficam declaradas |
| Versão em cabeçalho ou *media type* | URL estável | Menos visível; pior para cache e para testar no navegador |
| **Nativo do Spring 7, por caminho** | URL explícita; versões suportadas centralizadas; v2 convive por método | Recurso novo, com pouco material de referência |

## Consequências
- **Positivas:** introduzir uma v2 é declarar `version = "2"` no método que mudou e incluí-la em `supported`.
- **Negativas / custos:** todo controller precisa do `{version}` no caminho; quem não conhece o recurso pode estranhar.
- **O que observar:** o contrato OpenAPI documentando corretamente as versões; a convivência com o roteamento do Gateway na F02.
