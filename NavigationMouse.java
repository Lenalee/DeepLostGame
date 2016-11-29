package mygame1;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;

/**
 * Test 
 */
public class NavigationMouse extends SimpleApplication implements AnalogListener, ActionListener {
    
    private final int WIDTH = 2;
    Labyrint l = new Labyrint(21,21, 0);
    private byte[][] map = l.get_labyrint();; 
    private float w = 0, h = 0, speed = 10; 
    private BulletAppState bulletAppState;
    private RigidBodyControl floorPhy, wallPhy, ballPhy; // fyzicke controllery srazky objektu
    private Node  playerNode = new Node("the player"), world;
    private BetterCharacterControl playerControl;
    private CameraNode camNode;
    private Vector3f walkDirection = new Vector3f(0, 0, 0), viewDirection = new Vector3f(0, 0, 1);
    private boolean left = false, right = false, forward = false,
            backward = false, mouseRotateLeft = false, mouseRotateRight = false, mapShow = false;; 
    Geometry floorGeo;    
    private Sphere ballMesh;
    private BitmapText distanceText, coordinateText, bulletText, mapText; // texty
    private int health = 100, bullets = 30;
    private Vector3f lightDir = new Vector3f(-0.39f, -0.32f, -0.74f);
    static NavigationMouse app;

    public static void main(String[] args) {
        app = new NavigationMouse();
        app.start();
    }

    /**
     *  co se stane pri spusteni programu
     */
    @Override
    public void simpleInitApp() {
        attachText();
        attachCenterMark();
        ballMesh = new Sphere(32, 32, 0.25f, true, false);
        ballMesh.setTextureMode(Sphere.TextureMode.Projected);
        world = new Node("World");
        rootNode.attachChild(world);
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        world.attachChild(initFloor());        
        world.addLight(initLight());
        // create a skybox in code
        Texture west = assetManager.loadTexture("Textures/sky/skybox_left.png");
        Texture east = assetManager.loadTexture("Textures/sky/skybox_right.png");
        Texture north = assetManager.loadTexture("Textures/sky/skybox_back.png");
        Texture south = assetManager.loadTexture("Textures/sky/skybox_front.png");
        Texture up = assetManager.loadTexture("Textures/sky/skybox_up.png");
        Texture down = assetManager.loadTexture("Textures/sky/skybox_down.png");
        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        world.attachChild(sky);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        WaterFilter water = new WaterFilter(world, lightDir);
        fpp.addFilter(water);  
        // prochazi se polem a vykresluje se bud zed a nebo mezera 
        for (byte y = 0; y < map.length; y++) {
            for (byte x = 0; x < map[y].length; x++) {
                if (y == 0 && x == 1) {
                } else if (map[y][x] == 1 || map[y][x] == 2) {
                    makeWall(w, h);
                }   
                w += 4;
            }
            w = 0;
            h +=4;
        }
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(20.0f, 5.0f, 20.0f));
        world.addLight(sun);
        floorPhy = new RigidBodyControl(0.0f); // kdyz dam vic jak 0.0, tak me to odnasi nahoru(efekt) protoze podlaha pada za vlivu gravitace
        floorGeo.addControl(floorPhy);
       // klouze to hodne floorPhy.setFriction(0f);
        bulletAppState.getPhysicsSpace().add(floorPhy);
        playerNode.setLocalTranslation(new Vector3f(4, 0, 4));
        world.attachChild(playerNode);        
        playerControl = new BetterCharacterControl(0.5f, 1f, 30f);
        playerControl.setJumpForce(new Vector3f(0, 300, 0));
        playerControl.setGravity(new Vector3f(0, -10, 0));        
        playerNode.addControl(playerControl);
        bulletAppState.getPhysicsSpace().add(playerControl);
        // 1. nastavovani firstperson navigation
        camNode = new CameraNode("CamNode", cam);
        //Setting the direction to Spatial to camera, this means the camera will copy the movements of the Node
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        //attaching the camNode to the teaNode
        playerNode.attachChild(camNode);
        camNode.setQueueBucket(RenderQueue.Bucket.Gui);
        camNode.setLocalTranslation(new Vector3f(0f, 5f, -0.8f));
        Quaternion quat = new Quaternion();
        quat.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        camNode.setLocalRotation(quat);
        camNode.setEnabled(true);
        flyCam.setEnabled(false);     
        // zareistrovani listeneru
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("map", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("shoot", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(this, "Left", "Right", "Forward", "Back", "Jump", "map", "shoot");
        inputManager.addMapping("mouseRotateRight", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("mouseRotateLeft", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addListener(this, "mouseRotateRight", "mouseRotateLeft");
    }

    /** 
     * vytvori zakladni svetlo a vrati ho 
    */
    public AmbientLight initLight() {
        AmbientLight ambient = new AmbientLight();
        return ambient;
    }
    
    /**
     * vytvori velky kvadr, nastavi mu texturu, pozici a vrati to
     * reprezentuje podlahu
     */
    public Geometry initFloor() {
        Box floorMesh = new Box(80f, 2f, 80f);
        floorGeo = new Geometry("Floor", floorMesh);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture floor = assetManager.loadTexture("Textures/s.jpg");
        floorMat.setTexture("DiffuseMap", floor);
        floorGeo.setMaterial(floorMat);
        floorGeo.setLocalTranslation(30.0f, -0.5f, 30.0f);
        return floorGeo;
    }
    
    /**
     * float x: - x-ova souradnice kvadru zdi
     * float z: - z-ova souradnice kvadru zdi
     * vytvari objekt typu geometry, priradi mu material s texturou, nastavi pozici podle argumentu a vrati geometry(zed)
     */
    public void makeWall(float x, float z) {
        Box wallMesh = new Box(2f, 5f, 2f);
        Geometry wallGeo = new Geometry("Wall", wallMesh);
        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture wall = assetManager.loadTexture("Textures/god.png");
        wall.setWrap(Texture.WrapMode.Repeat);
        wallMat.setTexture("DiffuseMap", wall);
        wallGeo.setMaterial(wallMat);
        wallGeo.setLocalTranslation(x, 6.5f, z);
        world.attachChild(wallGeo);
        wallPhy = new RigidBodyControl(50000f); // v (kg), kdyz vetsi nez nula, tak jsou tzv. dynamicke, to znamena, ze na ne pusobi gravitace, sily, atd, cim bliz nule, tak tim lehci
        wallGeo.addControl(wallPhy);    
//  brickPhy.setFriction(0.20f); cim mensi, tim vic na ne pusobi sila, mensi treni
        bulletAppState.getPhysicsSpace().add(wallPhy);
    }
    
   @Override
    public void simpleUpdate(float tpf) {
        inputManager.setCursorVisible(false);
        if (playerNode.getLocalTranslation().y < -2 ) {
            health -= 1;
        }
        if (playerNode.getLocalTranslation().y < -15) {
           app.stop();
           System.out.println("You DIED!!!!!!!!!!!");
        }
        if (bullets == 0) {
            inputManager.deleteMapping("shoot");
        }
        distanceText.setText("Health: " + health);
        coordinateText.setText("Position (x, y, z) : " + Math.round(playerNode.getLocalTranslation().x) + " ; " + 
                Math.round(playerNode.getLocalTranslation().y) + " ; " + 
                Math.round(playerNode.getLocalTranslation().z));
        bulletText.setText("Remaining bullets: " + bullets);
        listener.setLocation(cam.getLocation());
        listener.setRotation(cam.getRotation());
        // Get current forward and left vectors of the playerNode:
        Vector3f modelForwardDir = playerNode.getWorldRotation().mult(Vector3f.UNIT_Z);
        Vector3f modelLeftDir = playerNode.getWorldRotation().mult(Vector3f.UNIT_X);
        // determine the change in direction
        walkDirection.set(0, 0, 0);
        if (forward) {
            walkDirection.addLocal(modelForwardDir.mult(speed));
        } else if (backward) {
            walkDirection.addLocal(modelForwardDir.mult(speed).negate());           
        } else if (left) {
            walkDirection.addLocal(modelForwardDir.crossLocal(Vector3f.UNIT_Y).multLocal(-speed * tpf).mult(4 * speed));
        } else if (right) {
            walkDirection.addLocal(modelForwardDir.crossLocal(Vector3f.UNIT_Y).multLocal(speed * tpf).mult(4 * speed));
        }
        playerControl.setWalkDirection(walkDirection); // walk!
        if (mouseRotateRight) {
            Quaternion rotateR = new Quaternion().fromAngleAxis(FastMath.PI * tpf, Vector3f.UNIT_Y);
            rotateR.multLocal(viewDirection);
            mouseRotateLeft = false;
            mouseRotateRight = false;
        } else if (mouseRotateLeft) {
            Quaternion rotateL = new Quaternion().fromAngleAxis(-FastMath.HALF_PI * tpf, Vector3f.UNIT_Y);
            rotateL.multLocal(viewDirection);
            mouseRotateLeft = false;
            mouseRotateRight = false;
        }
        playerControl.setViewDirection(viewDirection); // turn      
        if (mapShow) {
            String mapa = "";
        int posX = (int) (playerNode.getLocalTranslation().x/4);
        int posY = (int) (playerNode.getLocalTranslation().z/4);
        for (byte y = 0; y < map.length; y++) {
            for (byte x = 0; x < map[y].length; x++) {
                byte c = 0;
                if (x == posX && y == posY) {
                    mapa += " X ";
                } else {
                    if (x == map[y].length - 1) {
                       if (c > map[y].length - 10) {
                        mapa +=  "                     ";
                        } 
                    }
                    if ((map[y][x] == 1 || map[y][x] == 2)) {
                        mapa += " # ";
                    }  else {
                        mapa += "    ";
                        c++;
                    }  
                }
            }
            mapa += "\n";
        }
        mapText.setText(mapa);
            guiNode.attachChild(mapText);
            
        } else {
            guiNode.detachChild(mapText);
        }
    }

    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("mouseRotateLeft")) {
            mouseRotateLeft = true;
            mouseRotateRight = false;
        } else if (name.equals("mouseRotateRight")) {
            mouseRotateRight = true;
            mouseRotateLeft = false;  
        }
    }

    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("Left")) {
            left = keyPressed;
        } else if (name.equals("Right")) {
            right = keyPressed;
        } else if (name.equals("Forward")) {
            forward = keyPressed;
        } else if (name.equals("Back")) {
            backward = keyPressed;
        } else if (name.equals("Jump")) {
            playerControl.jump(); 
        } else if (name.equals("shoot") && !keyPressed) {
            shootCannonBall();
            bullets--;
        } else if (name.equals("map")) {
            mapShow = keyPressed;
        }
    }
  
    /**
     *  vytvori kulicku, nastavi hmotnost a rychlost, prida do bulletAppState
     */
    public void shootCannonBall() {
        Geometry ballGeo = new Geometry("cannon ball", ballMesh);
        Material stoneMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        stoneMat.setColor("Color", ColorRGBA.Blue);
        ballGeo.setMaterial(stoneMat);
        ballGeo.setLocalTranslation(cam.getLocation());
        world.attachChild(ballGeo);
        ballPhy = new RigidBodyControl(5f);
        ballGeo.addControl(ballPhy); // nevim, asi to zmenili, ale bylo to spatne v tutorialu
        ballPhy.setCcdSweptSphereRadius(0.1f);
        ballPhy.setCcdMotionThreshold(0.001f);
        ballPhy.setLinearVelocity(cam.getDirection().mult(50)); // cim vic, tim vic destruktivni
        bulletAppState.getPhysicsSpace().add(ballPhy);
    }
    
    /**
     * pomoci metody myBox vytvori 2 Boxy, zmensi je, nastavi soradnice na obrazovce a prida do guiNode 
     */
    private void attachCenterMark() {
        Geometry c = myBox("center mark", Vector3f.ZERO, ColorRGBA.White, 3, 0.5f);
        c.scale(4);
        c.setLocalTranslation(settings.getWidth() / 2, settings.getHeight() / 2, 0);
        guiNode.attachChild(c);
        Geometry d = myBox("center mark", Vector3f.ZERO, ColorRGBA.White, 0.5f, 3);
        d.scale(4);
        d.setLocalTranslation(settings.getWidth() / 2, settings.getHeight() / 2, 0);
        guiNode.attachChild(d);
    }
    
    /**
     * metoda vytvori objekt typu Box a vrati jej
     * @param name - jmeno Boxu 
     * @param loc - souradnice, kde se ma nachazet
     * @param color - barva
     * @param x - x-osova velikost Boxu
     * @param y - y-onova velikost Boxu
     * @return objekt typu Box
     */
    public Geometry myBox(String name, Vector3f loc, ColorRGBA color, float x, float y) {
        Box mesh = new Box(Vector3f.ZERO,x ,y ,1);
        Geometry geom = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        geom.setLocalTranslation(loc);
        return geom;
    }
    
    /**
     * metoda, ktera vytvari instance objektu textu, nastavuje jim polohu, font a prida do guiNode
     */
    private void attachText() {
       guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt"); // nacteni fontu, potrebneho k vytvoreni bitmaptext objektu
        distanceText = new BitmapText(assetManager.loadFont("Interface/Fonts/SnapITC.fnt"));
        distanceText.move(20, // posunuti x
                settings.getHeight() - 80,       // y
                0); 
        guiNode.attachChild(distanceText); 
        coordinateText = new BitmapText(assetManager.loadFont("Interface/Fonts/SnapITC.fnt"));
        coordinateText.move(20, settings.getHeight() - 50, 0); 
        guiNode.attachChild(coordinateText); 
        bulletText = new BitmapText(assetManager.loadFont("Interface/Fonts/SnapITC.fnt"));
        bulletText.move(20, settings.getHeight() - 110, 0); 
        guiNode.attachChild(bulletText);
        mapText = new BitmapText(assetManager.loadFont("Interface/Fonts/SnapITC.fnt"));
        mapText.move(settings.getWidth()/2 - 120, settings.getHeight() - 150, 0); 
    }
}
