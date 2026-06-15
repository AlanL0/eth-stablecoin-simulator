export type ProtocolPreset = {
  name: string;
  displayName: string;
  targetCollateralRatio: number;
  liquidationRatio: number;
  stabilityFeePct: number;
};

export const PROTOCOL_PRESETS: ProtocolPreset[] = [
  {
    name: "maker_sky",
    displayName: "Maker/Sky-style vault",
    targetCollateralRatio: 1.8,
    liquidationRatio: 1.5,
    stabilityFeePct: 5,
  },
  {
    name: "liquity",
    displayName: "Liquity-style borrowing",
    targetCollateralRatio: 2.0,
    liquidationRatio: 1.1,
    stabilityFeePct: 0.5,
  },
  {
    name: "aave_gho",
    displayName: "Aave/GHO-style borrowing",
    targetCollateralRatio: 2.2,
    liquidationRatio: 1.25,
    stabilityFeePct: 4,
  },
  {
    name: "custom",
    displayName: "Custom",
    targetCollateralRatio: 2.0,
    liquidationRatio: 1.5,
    stabilityFeePct: 5,
  },
];

export function getProtocolPreset(name: string): ProtocolPreset {
  return PROTOCOL_PRESETS.find((preset) => preset.name === name) ?? PROTOCOL_PRESETS[0];
}