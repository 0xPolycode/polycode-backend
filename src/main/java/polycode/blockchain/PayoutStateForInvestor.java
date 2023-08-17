package polycode.blockchain;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint256;
import java.math.BigInteger;

public class PayoutStateForInvestor extends StaticStruct {
    public BigInteger payoutId;

    public String investor;

    public BigInteger amountClaimed;

    public PayoutStateForInvestor(BigInteger payoutId, String investor, BigInteger amountClaimed) {
        super(new Uint256(payoutId), new Address(investor), new Uint256(amountClaimed));
        this.payoutId = payoutId;
        this.investor = investor;
        this.amountClaimed = amountClaimed;
    }

    public PayoutStateForInvestor(Uint256 payoutId, Address investor, Uint256 amountClaimed) {
        super(payoutId, investor, amountClaimed);
        this.payoutId = payoutId.getValue();
        this.investor = investor.getValue();
        this.amountClaimed = amountClaimed.getValue();
    }
}
