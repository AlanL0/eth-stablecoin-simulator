from agents.services.chart_intent import classify


def test_yield_over_time_intent_selects_projection_tool():
    intent = classify("Show me a chart of yield over time")
    assert intent.supported is True
    assert intent.primary_tool == "get_simulation_projection_chart"


def test_liquidation_intent_selects_band_and_health_tools():
    intent = classify("How close am I to liquidation?")
    assert intent.supported is True
    assert "get_liquidation_band_chart" in intent.tool_names
    assert "get_health_ratio_chart" in intent.tool_names


def test_unsupported_bear_bull_scenario():
    intent = classify("Show me bear and bull ETH scenarios for my vault")
    assert intent.supported is False
    assert intent.normalized_label == "bear_bull_eth_scenarios"


def test_unsupported_historical_eth():
    intent = classify("Plot ETH price over the last year")
    assert intent.normalized_label == "historical_eth_price"