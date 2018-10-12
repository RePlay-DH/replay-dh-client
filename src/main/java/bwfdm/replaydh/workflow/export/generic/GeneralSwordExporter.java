package bwfdm.replaydh.workflow.export.generic;

import java.io.File;
import java.util.Map;

import org.swordapp.client.AuthCredentials;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;

import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository.SwordRequestType;

public class GeneralSwordExporter implements SwordCommons{

	@Override
	public AuthCredentials getNewAuthCredentials(String adminUser, char[] adminPassword, String userLogin) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocumentURL,
			AuthCredentials authCredentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends SwordResponse> T exportElement(String userLogin, String collectionURL,
			SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file,
			Map<String, String> metadataMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSwordAccessible() {
		// TODO Auto-generated method stub
		return false;
	}

	
	//TODO: add other implemented methods from DSpace_v6 and Dataverse_v4
	
}
