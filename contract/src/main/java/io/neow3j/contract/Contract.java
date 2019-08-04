package io.neow3j.contract;

import static io.neow3j.utils.Strings.isEmpty;

import io.neow3j.contract.abi.model.NeoContractEvent;
import io.neow3j.contract.abi.model.NeoContractFunction;
import io.neow3j.contract.abi.model.NeoContractInterface;
import io.neow3j.model.types.ContractParameterType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Contract {

    // TODO: 2019-07-24 Guil:
    // Apply the build pattern here?

    private final ScriptHash contractScriptHash;

    private ContractDeploymentScript deploymentScript;

    private NeoContractInterface abi;

    public Contract(ContractDeploymentScript deploymentScript) {
        this.deploymentScript = deploymentScript;
        this.contractScriptHash = deploymentScript.getContractScriptHash();
    }

    public Contract(ContractDeploymentScript deploymentScript, NeoContractInterface abi) {
        this.deploymentScript = deploymentScript;
        this.contractScriptHash = deploymentScript.getContractScriptHash();
        this.abi = abi;
    }

    /**
     * Creates a new contract with the given script hash
     *
     * @param contractScriptHash Contract script hash in little-endian order.
     * @deprecated
     */
    @Deprecated
    public Contract(byte[] contractScriptHash) {
        this.contractScriptHash = new ScriptHash(contractScriptHash);
    }

    /**
     * Creates a new contract with the given script hash and ABI.
     *
     * @param contractScriptHash Contract script hash in little-endian order.
     * @param abi The contract's ABI.
     * @deprecated
     */
    @Deprecated
    public Contract(byte[] contractScriptHash, NeoContractInterface abi) {
        this(contractScriptHash);
        this.abi = abi;
    }

    /**
     * Creates a new contract with the given script hash
     *
     * @param contractScriptHash Contract script hash.
     */
    public Contract(ScriptHash contractScriptHash) {
        this.contractScriptHash = contractScriptHash;
    }

    /**
     * Creates a new contract with the given script hash and ABI.
     *
     * @param contractScriptHash Contract script hash in little-endian order.
     * @param abi The contract's ABI.
     */
    public Contract(ScriptHash contractScriptHash, NeoContractInterface abi) {
        this(contractScriptHash);
        this.abi = abi;
    }

    public ScriptHash getContractScriptHash() {
        return contractScriptHash;
    }

    public ContractDeploymentScript getDeploymentScript() {
        return deploymentScript;
    }

    public NeoContractInterface getAbi() {
        return abi;
    }

    public Contract abi(NeoContractInterface abi) {
        this.abi = abi;
        return this;
    }

    public Optional<NeoContractFunction> getEntryPoint() {
        return getFunction(abi.getEntryPoint());
    }

    public List<ContractParameter> getEntryPointParameters() {
        return getFunctionParameters(abi.getEntryPoint());
    }

    public ContractParameterType getEntryPointReturnType() {
        return getFunctionReturnType(abi.getEntryPoint());
    }

    public List<NeoContractFunction> getFunctions() {
        return abi.getFunctions();
    }

    public List<NeoContractEvent> getEvents() {
        return abi.getEvents();
    }

    public List<ContractParameter> getFunctionParameters(final String functionName) {
        throwIfABINotSet();
        return abi.getFunctions()
            .stream()
            .filter(f -> !isEmpty(f.getName()))
            .filter(f -> f.getName().equals(functionName))
            .findFirst()
            .map(NeoContractFunction::getParameters)
            .orElse(Collections.EMPTY_LIST);
    }

    public Optional<NeoContractFunction> getFunction(final String functionName) {
        return abi.getFunctions()
            .stream()
            .filter(f -> !isEmpty(f.getName()))
            .filter(f -> f.getName().equals(functionName))
            .findFirst();
    }

    public ContractParameterType getFunctionReturnType(final String functionName) {
        throwIfABINotSet();
        return abi.getFunctions()
            .stream()
            .filter(f -> !isEmpty(f.getName()))
            .filter(f -> f.getName().equals(functionName))
            .findFirst()
            .map(NeoContractFunction::getReturnType)
            .orElseThrow(() -> new IllegalArgumentException(
                "No returnType found for the function (" + functionName + ")."));
    }

    public Optional<NeoContractEvent> getEvent(final String eventName) {
        return abi.getEvents()
            .stream()
            .filter(f -> !isEmpty(f.getName()))
            .filter(f -> f.getName().equals(eventName))
            .findFirst();
    }

    public List<ContractParameter> getEventParameters(final String eventName) {
        throwIfABINotSet();
        return getEvent(eventName)
            .map(NeoContractEvent::getParameters)
            .orElse(Collections.EMPTY_LIST);
    }

    private void throwIfABINotSet() {
        if (abi == null) {
            throw new IllegalStateException("ABI should be set first.");
        }
    }

}
