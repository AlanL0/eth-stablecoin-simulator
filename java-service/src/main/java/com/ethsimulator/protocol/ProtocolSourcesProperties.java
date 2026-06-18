package com.ethsimulator.protocol;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eth-simulator.protocol-sources")
public class ProtocolSourcesProperties {

    private long chainId = 1;
    private ChainlinkSourceProperties chainlink = new ChainlinkSourceProperties();
    private SkySourceProperties sky = new SkySourceProperties();
    private LiquitySourceProperties liquity = new LiquitySourceProperties();
    private AaveSourceProperties aave = new AaveSourceProperties();

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public ChainlinkSourceProperties getChainlink() {
        return chainlink;
    }

    public void setChainlink(ChainlinkSourceProperties chainlink) {
        this.chainlink = chainlink;
    }

    public SkySourceProperties getSky() {
        return sky;
    }

    public void setSky(SkySourceProperties sky) {
        this.sky = sky;
    }

    public LiquitySourceProperties getLiquity() {
        return liquity;
    }

    public void setLiquity(LiquitySourceProperties liquity) {
        this.liquity = liquity;
    }

    public AaveSourceProperties getAave() {
        return aave;
    }

    public void setAave(AaveSourceProperties aave) {
        this.aave = aave;
    }

    public static class ChainlinkSourceProperties extends ProtocolSourceProperties {
        private int staleThresholdSeconds = 3600;

        public int getStaleThresholdSeconds() {
            return staleThresholdSeconds;
        }

        public void setStaleThresholdSeconds(int staleThresholdSeconds) {
            this.staleThresholdSeconds = staleThresholdSeconds;
        }
    }

    public static class SkySourceProperties extends ProtocolSourceProperties {
        private String chainlog = "0xda0ab1e00181debC73375a6F9bad8EaF7422B683";
        private String jug = "0x19c0976f590D67707E62397C87829d896Dc0f1F1";
        private String vat = "0x35D1b3F3D7966A1DFe207aa4514C12a259A0492B";
        private String susds = "0xa3931d71877C0E7a3148CB7Eb4463524fec27fbd";
        private String borrowIlk = "ETH-A";

        public String getChainlog() {
            return chainlog;
        }

        public void setChainlog(String chainlog) {
            this.chainlog = chainlog;
        }

        public String getJug() {
            return jug;
        }

        public void setJug(String jug) {
            this.jug = jug;
        }

        public String getVat() {
            return vat;
        }

        public void setVat(String vat) {
            this.vat = vat;
        }

        public String getSusds() {
            return susds;
        }

        public void setSusds(String susds) {
            this.susds = susds;
        }

        public String getBorrowIlk() {
            return borrowIlk;
        }

        public void setBorrowIlk(String borrowIlk) {
            this.borrowIlk = borrowIlk;
        }
    }

    public static class LiquitySourceProperties extends ProtocolSourceProperties {
        private String wethActivePool = "0xacece9a6ff7fea9b9e1cdfeee61ca2b45cc4627b";
        private String wethStabilityPool = "0xf69eb8c0d95d4094c16686769460f678727393cf";
        private String boldToken = "0xb01dd87B29d187F3E3a4Bf6cdAebfb97F3D9aB98";
        private int savingsLookbackDays = 30;

        public String getWethActivePool() {
            return wethActivePool;
        }

        public void setWethActivePool(String wethActivePool) {
            this.wethActivePool = wethActivePool;
        }

        public String getWethStabilityPool() {
            return wethStabilityPool;
        }

        public void setWethStabilityPool(String wethStabilityPool) {
            this.wethStabilityPool = wethStabilityPool;
        }

        public String getBoldToken() {
            return boldToken;
        }

        public void setBoldToken(String boldToken) {
            this.boldToken = boldToken;
        }

        public int getSavingsLookbackDays() {
            return savingsLookbackDays;
        }

        public void setSavingsLookbackDays(int savingsLookbackDays) {
            this.savingsLookbackDays = savingsLookbackDays;
        }
    }

    public static class AaveSourceProperties extends ProtocolSourceProperties {
        private String pool = "0x87870Bca3F3fD6335C3F4ce8392D69350B4fA4E2";
        private String gho = "0x40D16FC0246aD3160Ccc09B8D0D3A2cD28aE6C2f";
        private String sgho = "0xE1753F2e00940cC31213dd92013cF019DFE4ca1d";

        public String getPool() {
            return pool;
        }

        public void setPool(String pool) {
            this.pool = pool;
        }

        public String getGho() {
            return gho;
        }

        public void setGho(String gho) {
            this.gho = gho;
        }

        public String getSgho() {
            return sgho;
        }

        public void setSgho(String sgho) {
            this.sgho = sgho;
        }
    }
}