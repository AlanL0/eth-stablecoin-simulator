package com.ethsimulator.protocol.abi;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint80;
import java.nio.charset.StandardCharsets;

/**
 * Minimal reviewed ABI selectors used by protocol adapters.
 */
public final class ProtocolAbi {

    private ProtocolAbi() {
    }

    public static final Function LATEST_ROUND_DATA = new Function(
            "latestRoundData",
            java.util.List.of(),
            java.util.List.of(
                    new TypeReference<Uint80>() {
                    },
                    new TypeReference<org.web3j.abi.datatypes.generated.Int256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    }
            )
    );

    public static final Function GET_ROUND_DATA = new Function(
            "getRoundData",
            java.util.List.of(new Uint80(0L)),
            java.util.List.of(
                    new TypeReference<Uint80>() {
                    },
                    new TypeReference<org.web3j.abi.datatypes.generated.Int256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    }
            )
    );

    public static final Function DECIMALS = new Function(
            "decimals",
            java.util.List.of(),
            java.util.List.of(new TypeReference<org.web3j.abi.datatypes.generated.Uint8>() {
            })
    );

    public static final Function JUG_BASE = new Function(
            "base",
            java.util.List.of(),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static final Function JUG_ILKS = new Function(
            "ilks",
            java.util.List.of(new Bytes32(new byte[32])),
            java.util.List.of(
                    new TypeReference<Uint256>() {
                    },
                    new TypeReference<Uint256>() {
                    }
            )
    );

    public static final Function SUSDS_SSR = new Function(
            "ssr",
            java.util.List.of(),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static final Function CHAINLOG_GET_ADDRESS = new Function(
            "getAddress",
            java.util.List.of(new Bytes32(new byte[32])),
            java.util.List.of(new TypeReference<Address>() {
            })
    );

    public static final Function AGG_WEIGHTED_DEBT_SUM = new Function(
            "aggWeightedDebtSum",
            java.util.List.of(),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static final Function AGG_RECORDED_DEBT = new Function(
            "aggRecordedDebt",
            java.util.List.of(),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static final Function GET_TOTAL_BOLD_DEPOSITS = new Function(
            "getTotalBoldDeposits",
            java.util.List.of(),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static final Function GET_RESERVE_DATA = new Function(
            "getReserveData",
            java.util.List.of(new Address("0x0")),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static final Function TARGET_RATE = new Function(
            "targetRate",
            java.util.List.of(),
            java.util.List.of(new TypeReference<Uint256>() {
            })
    );

    public static String selector(Function function) {
        return FunctionEncoder.encode(function).substring(0, 10);
    }

    public static Function jugIlks(String ilk) {
        return new Function(
                "ilks",
                java.util.List.of(new Bytes32(ilkToBytes32(ilk))),
                java.util.List.of(
                        new TypeReference<Uint256>() {
                        },
                        new TypeReference<Uint256>() {
                        }
                )
        );
    }

    public static Function chainlogGetAddress(String key) {
        return new Function(
                "getAddress",
                java.util.List.of(new Bytes32(ilkToBytes32(key))),
                java.util.List.of(new TypeReference<Address>() {
                })
        );
    }

    public static Function getRoundData(long roundId) {
        return new Function(
                "getRoundData",
                java.util.List.of(new Uint80(roundId)),
                java.util.List.of(
                        new TypeReference<Uint80>() {
                        },
                        new TypeReference<org.web3j.abi.datatypes.generated.Int256>() {
                        },
                        new TypeReference<Uint256>() {
                        },
                        new TypeReference<Uint256>() {
                        },
                        new TypeReference<Uint256>() {
                        }
                )
        );
    }

    public static Function getReserveData(String asset) {
        return new Function(
                "getReserveData",
                java.util.List.of(new Address(asset)),
                java.util.List.of(new TypeReference<Uint256>() {
                })
        );
    }

    public static byte[] ilkToBytes32(String ilk) {
        byte[] bytes = new byte[32];
        byte[] ilkBytes = ilk.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(ilkBytes, 0, bytes, 0, Math.min(ilkBytes.length, 32));
        return bytes;
    }
}