package bisq.httpapi.service.endpoint;

import bisq.httpapi.exceptions.NotFoundException;
import bisq.httpapi.facade.BackupFacade;
import bisq.httpapi.model.BackupList;
import bisq.httpapi.model.CreatedBackup;
import bisq.httpapi.util.ResourceHelper;

import bisq.common.UserThread;

import javax.inject.Inject;

import java.nio.file.FileAlreadyExistsException;

import java.io.FileNotFoundException;
import java.io.InputStream;



import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import javax.validation.ValidationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;


@Api(value = "backups", authorizations = @Authorization(value = "accessToken"))
@Produces(MediaType.APPLICATION_JSON)
public class BackupEndpoint {

    private final BackupFacade backupFacade;

    @Inject
    public BackupEndpoint(BackupFacade backupFacade) {
        this.backupFacade = backupFacade;
    }

    @ApiOperation(value = "List backups", response = BackupList.class)
    @GET
    public void getBackupList(@Suspended final AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                final BackupList backupList = new BackupList(backupFacade.getBackupList());
                asyncResponse.resume(backupList);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Create backup", response = CreatedBackup.class)
    @POST
    public void createBackup(@Suspended final AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                final CreatedBackup backup = new CreatedBackup(backupFacade.createBackup());
                asyncResponse.resume(backup);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation("Upload backup")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path("/upload")
    public void uploadBackup(@Suspended final AsyncResponse asyncResponse, @FormDataParam("file") InputStream uploadedInputStream,
                             @FormDataParam("file") FormDataContentDisposition fileDetail) {
        UserThread.execute(() -> {
            try {
                try {
                    backupFacade.uploadBackup(fileDetail.getFileName(), uploadedInputStream);
                    asyncResponse.resume(Response.noContent().build());
                } catch (FileAlreadyExistsException e) {
                    throw new ValidationException(e.getMessage());
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Get backup")
    @GET
    @Path("/{path}")
    public void getBackup(@Suspended final AsyncResponse asyncResponse, @PathParam("path") String fileName) {
        UserThread.execute(() -> {
            try {
                try {
                    final Response response = Response.ok(backupFacade.getBackup(fileName), MediaType.APPLICATION_OCTET_STREAM_TYPE)
                            .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                            .build();
                    asyncResponse.resume(response);
                } catch (FileNotFoundException e) {
                    final Response response = ResourceHelper.toValidationErrorResponse(e, 404).type(MediaType.APPLICATION_JSON).build();
                    asyncResponse.resume(response);
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Restore backup")
    @POST
    @Path("/{path}/restore")
    public void restoreBackup(@Suspended final AsyncResponse asyncResponse, @PathParam("path") String fileName) {
        UserThread.execute(() -> {
            try {
                try {
                    backupFacade.requestBackupRestore(fileName);
                    asyncResponse.resume(Response.noContent().build());
                } catch (FileNotFoundException e) {
                    throw new NotFoundException(e.getMessage());
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Remove backup")
    @DELETE
    @Path("/{path}")
    public void removeBackup(@Suspended final AsyncResponse asyncResponse, @PathParam("path") String fileName) {
        UserThread.execute(() -> {
            try {
                try {
                    if (backupFacade.removeBackup(fileName))
                        asyncResponse.resume(Response.noContent().build());
                    else
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unable to remove file: " + fileName).build());
                } catch (FileNotFoundException e) {
                    asyncResponse.resume(ResourceHelper.toValidationErrorResponse(e, 404).build());
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}