package bisq.httpapi.service.endpoint;

import bisq.httpapi.facade.NetworkFacade;
import bisq.httpapi.model.BitcoinNetworkStatus;
import bisq.httpapi.model.P2PNetworkStatus;

import bisq.common.UserThread;

import javax.inject.Inject;



import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;


@Api(value = "network", authorizations = @Authorization(value = "accessToken"))
@Produces(MediaType.APPLICATION_JSON)
public class NetworkEndpoint {

    private final NetworkFacade networkFacade;

    @Inject
    public NetworkEndpoint(NetworkFacade networkFacade) {
        this.networkFacade = networkFacade;
    }

    @ApiOperation(value = "Get Bitcoin network status", response = BitcoinNetworkStatus.class)
    @GET
    @Path("/bitcoin/status")
    public void getBitcoinNetworkStatus(@Suspended final AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                asyncResponse.resume(networkFacade.getBitcoinNetworkStatus());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Get P2P network status", response = P2PNetworkStatus.class)
    @GET
    @Path("/p2p/status")
    public void getP2PNetworkStatus(@Suspended final AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                asyncResponse.resume(networkFacade.getP2PNetworkStatus());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
