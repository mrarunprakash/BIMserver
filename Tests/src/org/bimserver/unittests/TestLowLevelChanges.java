package org.bimserver.unittests;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.activation.DataHandler;

import nl.tue.buildingsmart.emf.SchemaLoader;

import org.bimserver.Server;
import org.bimserver.ServerInitializer;
import org.bimserver.emf.IdEObject;
import org.bimserver.ifc.step.deserializer.IfcStepDeserializer;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.interfaces.objects.SSerializer;
import org.bimserver.interfaces.objects.SUserType;
import org.bimserver.models.ifc2x3.IfcWall;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.serializers.IfcModelInterface;
import org.bimserver.shared.SDownloadResult;
import org.bimserver.shared.ServerException;
import org.bimserver.shared.ServiceInterface;
import org.bimserver.shared.UserException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLowLevelChanges {
	
	private static Server server;
	private static ServiceInterface service;
	private static SProject project;

	@BeforeClass
	public static void setup() {
		server = new Server();
		server.start("127.0.0.1", 8082, "home", "../BimServer/www");
		service = ServerInitializer.getSystemService();
		createUserAndLogin();
		
	}
	
	@AfterClass
	public static void shutdown() {
		server.stop();
	}
	
	private static long createUserAndLogin() {
		int nextInt = new Random().nextInt();
		try {
			String username = "test" + nextInt + "@bimserver.org";
			long addUser = service.addUser(username, "User " + nextInt, SUserType.USER, false);
			service.changePassword(addUser, null, "test");
			project = service.addProject("Project " + new Random().nextInt());
			service.addUserToProject(addUser, project.getOid());
			service.login(username, "test");
			return addUser;
		} catch (UserException e) {
			e.printStackTrace();
		} catch (ServerException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	@Test
	public void testCreateObject() {
		try {
			service.startTransaction(project.getId());
			long wallOid = service.createObject("IfcWall");
			long roid = service.commitTransaction();
			IfcModelInterface model = getSingleRevision(roid);
			if (model.size() != 1) {
				fail("1 object expected, found " + model.size());
			}
			IdEObject idEObject = model.getValues().iterator().next();
			if (!(idEObject instanceof IfcWall)) {
				fail("Object should be of type IfcWall but is " + idEObject.eClass().getName());
			}
			if (idEObject.getOid() != wallOid) {
				fail("Oids don't match " + idEObject.getOid() + ", " + wallOid);
			}
		} catch (UserException e) {
			fail(e.getMessage());
		} catch (ServerException e) {
			fail(e.getMessage());
		} catch (DeserializeException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSetAttribute() {
		try {
			service.startTransaction(project.getId());
			long wallOid = service.createObject("IfcWindow");
			service.setAttribute(wallOid, "IfcWall", "OverallHeight", 200);
			long roid = service.commitTransaction();
			IfcModelInterface model = getSingleRevision(roid);
			if (model.size() != 1) {
				fail("1 object expected, found " + model.size());
			}
			IdEObject idEObject = model.getValues().iterator().next();
			if (!(idEObject instanceof IfcWall)) {
				fail("Object should be of type IfcWall but is " + idEObject.eClass().getName());
			}
			if (idEObject.getOid() != wallOid) {
				fail("Oids don't match " + idEObject.getOid() + ", " + wallOid);
			}
		} catch (UserException e) {
			fail(e.getMessage());
		} catch (ServerException e) {
			fail(e.getMessage());
		} catch (DeserializeException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	private IfcModelInterface getSingleRevision(long roid) throws UserException, ServerException, DeserializeException, IOException {
		SRevision revision = service.getRevision(roid);
		SSerializer serializerByContentType = service.getSerializerByContentType("application/ifc");
		int downloadId = service.download(revision.getOid(), serializerByContentType.getName(), true);
		SDownloadResult downloadData = service.getDownloadData(downloadId);
		DataHandler dataHandler = downloadData.getFile();
		IfcStepDeserializer deserializer = new IfcStepDeserializer();
		deserializer.init(SchemaLoader.loadDefaultSchema());
		IfcModelInterface model = deserializer.read(dataHandler.getInputStream(), true, 0);
		return model;
	}
}