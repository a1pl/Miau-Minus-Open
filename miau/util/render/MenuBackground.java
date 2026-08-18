package miau.util.render;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class MenuBackground {
    private static final String SHADER_FBM = "#version 120\nuniform float time;\nuniform vec2 resolution;\nfloat random(vec2 st){return fract(sin(dot(st.xy,vec2(12.9898,78.233)))*43758.5453123);}\nfloat noise(vec2 st){vec2 i=floor(st);vec2 f=fract(st);float a=random(i);float b=random(i+vec2(1,0));float c=random(i+vec2(0,1));float d=random(i+vec2(1,1));vec2 u=f*f*(3.-2.*f);return mix(a,b,u.x)+(c-a)*u.y*(1.-u.x)+(d-b)*u.x*u.y;}\nfloat fbm(vec2 st){float v=0.;float a=0.5;mat2 rot=mat2(cos(.5),sin(.5),-sin(.5),cos(.5));for(int i=0;i<5;++i){v+=a*noise(st);st=rot*st*2.+vec2(100);a*=.5;}return v;}\nvoid main(){\n  vec2 st=gl_FragCoord.xy/resolution*3.;st.x*=resolution.x/resolution.y;\n  vec2 q=vec2(fbm(st),fbm(st+vec2(1)));\n  vec2 r=vec2(fbm(st+q+vec2(1.7,9.2)+.15*time),fbm(st+q+vec2(8.3,2.8)+.126*time));\n  float f=fbm(st+r);\n  vec3 col=mix(vec3(.102,.62,.667),vec3(.667,.667,.498),clamp(f*f*4.,0.,1.));\n  col=mix(col,vec3(0,0,.165),clamp(length(q),0.,1.));\n  col=mix(col,vec3(.667,1,1),clamp(length(r.x),0.,1.));\n  col*=.6;col=pow(col,vec3(1.2));\n  gl_FragColor=vec4(col,1);\n}";
    private static final String SHADER_RAYMARCHING = "#version 120\nuniform vec2 resolution;\nuniform float time;\nmat2 m(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}\nfloat map(vec3 p){p.xz*=m(time*.4);p.xy*=m(time*.1);vec3 q=p*2.+time;return length(p+vec3(sin(time*.7)))*log(length(p)+1.)+sin(q.x+sin(q.z+sin(q.y)))*.5-1.;}\nvoid main(){vec2 a=gl_FragCoord.xy/resolution.y-vec2(.9,.5);vec3 cl=vec3(0);float d=2.5;for(int i=0;i<=5;i++){vec3 p=vec3(0,0,4)+normalize(vec3(a,-1))*d;float rz=map(p);float f=clamp((rz-map(p+.1))*.5,-.1,1.);vec3 l=vec3(.1,.3,.4)+vec3(5,2.5,3)*f;cl=cl*l+smoothstep(2.5,0.,rz)*.6*l;d+=min(rz,1.);}gl_FragColor=vec4(cl,1);}";
    private static final String SHADER_TUNNEL = "#version 120\nuniform float time;\nuniform vec2 resolution;\n#define PI 3.14\nmat2 rot(float a){return mat2(cos(a),-sin(a),sin(a),cos(a));}\nvoid main(){vec2 p=(gl_FragCoord.xy*2.-resolution)/min(resolution.x,resolution.y);p=rot(time*.94*PI)*p;float t;if(sin(time)==1.)t=.075/abs(1.-length(p));else t=.075/abs(.8-length(p));gl_FragColor=vec4(vec3(t)*vec3(.13*(sin(time)+12.),p.y*1.7,3.5),1);}";
    private static final String SHADER_AURORA = "#version 120\nuniform float time;\nuniform vec2 resolution;\nvoid main(){\n  vec2 uv=gl_FragCoord.xy/resolution;\n  float t=time*.3;\n  vec3 col=vec3(0);\n  for(int i=0;i<4;i++){\n    float fi=float(i);\n    float wave=sin(uv.x*3.+t+fi*1.3)*0.5+0.5;\n    float band=smoothstep(.0,.15,uv.y-wave*.4+fi*.12)*smoothstep(.35,0.2,uv.y-wave*.4+fi*.12);\n    vec3 bandCol=mix(vec3(0.,.8,.6),vec3(.1,.3,.9),fi/4.+sin(t+fi)*.3);\n    col+=bandCol*band*.5;\n  }\n  col+=vec3(.02,.03,.06);\n  col*=1.-0.5*dot(uv-.5,uv-.5);\n  gl_FragColor=vec4(col,1);\n}";
    private static final String SHADER_SMOKE = "#version 120\nuniform float time;\nuniform vec2 resolution;\nfloat hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\nfloat noise(vec2 p){vec2 i=floor(p);vec2 f=fract(p);vec2 u=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),u.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1)),u.x),u.y);}\nvoid main(){\n  vec2 uv=gl_FragCoord.xy/resolution;\n  float t=time*.12;\n  float n=0.;\n  float amp=.5;vec2 freq=vec2(2);\n  for(int i=0;i<5;i++){n+=noise(uv*freq+vec2(0,t))*amp;freq*=2.1;amp*=.5;}\n  vec3 dark=vec3(.04,.04,.08);\n  vec3 mid=vec3(.08,.12,.2);\n  vec3 bright=vec3(.15,.25,.4);\n  vec3 col=mix(dark,mid,n);\n  col=mix(col,bright,n*n);\n  gl_FragColor=vec4(col,1);\n}";
    private static final String SHADER_NEBULA = "#version 120\nuniform float time;\nuniform vec2 resolution;\nfloat hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);}\nfloat noise(vec2 p){vec2 i=floor(p);vec2 f=fract(p);vec2 u=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),u.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),u.x),u.y);}\nfloat fbm(vec2 p){float v=0.;float a=.5;for(int i=0;i<5;i++){v+=a*noise(p);p=p*2.+vec2(17.);a*=.5;}return v;}\nvoid main(){\n  vec2 uv=gl_FragCoord.xy/resolution;\n  vec2 p=uv*1.6-vec2(.8);\n  float t=time*.12;\n  float n=fbm(p*1.4+vec2(t,t*.7+fbm(p*2.+vec2(-t*.3,t))));\n  vec3 c1=vec3(.3,.1,.6);\n  vec3 c2=vec3(0.,.6,.8);\n  vec3 c3=vec3(.9,.3,.5);\n  vec3 col=mix(c1,c2,n);\n  col=mix(col,c3,n*n);\n  col*=pow(1.-dot(uv-.5,uv-.5)*1.5,.8);\n  col+=vec3(.03,.03,.06);\n  gl_FragColor=vec4(col,1);\n}";
    private static final String[][] SHADERS = new String[][]{
        {
                "FBM",
                "#version 120\nuniform float time;\nuniform vec2 resolution;\nfloat random(vec2 st){return fract(sin(dot(st.xy,vec2(12.9898,78.233)))*43758.5453123);}\nfloat noise(vec2 st){vec2 i=floor(st);vec2 f=fract(st);float a=random(i);float b=random(i+vec2(1,0));float c=random(i+vec2(0,1));float d=random(i+vec2(1,1));vec2 u=f*f*(3.-2.*f);return mix(a,b,u.x)+(c-a)*u.y*(1.-u.x)+(d-b)*u.x*u.y;}\nfloat fbm(vec2 st){float v=0.;float a=0.5;mat2 rot=mat2(cos(.5),sin(.5),-sin(.5),cos(.5));for(int i=0;i<5;++i){v+=a*noise(st);st=rot*st*2.+vec2(100);a*=.5;}return v;}\nvoid main(){\n  vec2 st=gl_FragCoord.xy/resolution*3.;st.x*=resolution.x/resolution.y;\n  vec2 q=vec2(fbm(st),fbm(st+vec2(1)));\n  vec2 r=vec2(fbm(st+q+vec2(1.7,9.2)+.15*time),fbm(st+q+vec2(8.3,2.8)+.126*time));\n  float f=fbm(st+r);\n  vec3 col=mix(vec3(.102,.62,.667),vec3(.667,.667,.498),clamp(f*f*4.,0.,1.));\n  col=mix(col,vec3(0,0,.165),clamp(length(q),0.,1.));\n  col=mix(col,vec3(.667,1,1),clamp(length(r.x),0.,1.));\n  col*=.6;col=pow(col,vec3(1.2));\n  gl_FragColor=vec4(col,1);\n}"
        },
        {
                "Raymarching",
                "#version 120\nuniform vec2 resolution;\nuniform float time;\nmat2 m(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}\nfloat map(vec3 p){p.xz*=m(time*.4);p.xy*=m(time*.1);vec3 q=p*2.+time;return length(p+vec3(sin(time*.7)))*log(length(p)+1.)+sin(q.x+sin(q.z+sin(q.y)))*.5-1.;}\nvoid main(){vec2 a=gl_FragCoord.xy/resolution.y-vec2(.9,.5);vec3 cl=vec3(0);float d=2.5;for(int i=0;i<=5;i++){vec3 p=vec3(0,0,4)+normalize(vec3(a,-1))*d;float rz=map(p);float f=clamp((rz-map(p+.1))*.5,-.1,1.);vec3 l=vec3(.1,.3,.4)+vec3(5,2.5,3)*f;cl=cl*l+smoothstep(2.5,0.,rz)*.6*l;d+=min(rz,1.);}gl_FragColor=vec4(cl,1);}"
        },
        {
                "Tunnel",
                "#version 120\nuniform float time;\nuniform vec2 resolution;\n#define PI 3.14\nmat2 rot(float a){return mat2(cos(a),-sin(a),sin(a),cos(a));}\nvoid main(){vec2 p=(gl_FragCoord.xy*2.-resolution)/min(resolution.x,resolution.y);p=rot(time*.94*PI)*p;float t;if(sin(time)==1.)t=.075/abs(1.-length(p));else t=.075/abs(.8-length(p));gl_FragColor=vec4(vec3(t)*vec3(.13*(sin(time)+12.),p.y*1.7,3.5),1);}"
        },
        {
                "Aurora",
                "#version 120\nuniform float time;\nuniform vec2 resolution;\nvoid main(){\n  vec2 uv=gl_FragCoord.xy/resolution;\n  float t=time*.3;\n  vec3 col=vec3(0);\n  for(int i=0;i<4;i++){\n    float fi=float(i);\n    float wave=sin(uv.x*3.+t+fi*1.3)*0.5+0.5;\n    float band=smoothstep(.0,.15,uv.y-wave*.4+fi*.12)*smoothstep(.35,0.2,uv.y-wave*.4+fi*.12);\n    vec3 bandCol=mix(vec3(0.,.8,.6),vec3(.1,.3,.9),fi/4.+sin(t+fi)*.3);\n    col+=bandCol*band*.5;\n  }\n  col+=vec3(.02,.03,.06);\n  col*=1.-0.5*dot(uv-.5,uv-.5);\n  gl_FragColor=vec4(col,1);\n}"
        },
        {
                "Smoke",
                "#version 120\nuniform float time;\nuniform vec2 resolution;\nfloat hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\nfloat noise(vec2 p){vec2 i=floor(p);vec2 f=fract(p);vec2 u=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),u.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1)),u.x),u.y);}\nvoid main(){\n  vec2 uv=gl_FragCoord.xy/resolution;\n  float t=time*.12;\n  float n=0.;\n  float amp=.5;vec2 freq=vec2(2);\n  for(int i=0;i<5;i++){n+=noise(uv*freq+vec2(0,t))*amp;freq*=2.1;amp*=.5;}\n  vec3 dark=vec3(.04,.04,.08);\n  vec3 mid=vec3(.08,.12,.2);\n  vec3 bright=vec3(.15,.25,.4);\n  vec3 col=mix(dark,mid,n);\n  col=mix(col,bright,n*n);\n  gl_FragColor=vec4(col,1);\n}"
        },
        {
                "Nebula",
                "#version 120\nuniform float time;\nuniform vec2 resolution;\nfloat hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);}\nfloat noise(vec2 p){vec2 i=floor(p);vec2 f=fract(p);vec2 u=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),u.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),u.x),u.y);}\nfloat fbm(vec2 p){float v=0.;float a=.5;for(int i=0;i<5;i++){v+=a*noise(p);p=p*2.+vec2(17.);a*=.5;}return v;}\nvoid main(){\n  vec2 uv=gl_FragCoord.xy/resolution;\n  vec2 p=uv*1.6-vec2(.8);\n  float t=time*.12;\n  float n=fbm(p*1.4+vec2(t,t*.7+fbm(p*2.+vec2(-t*.3,t))));\n  vec3 c1=vec3(.3,.1,.6);\n  vec3 c2=vec3(0.,.6,.8);\n  vec3 c3=vec3(.9,.3,.5);\n  vec3 col=mix(c1,c2,n);\n  col=mix(col,c3,n*n);\n  col*=pow(1.-dot(uv-.5,uv-.5)*1.5,.8);\n  col+=vec3(.03,.03,.06);\n  gl_FragColor=vec4(col,1);\n}"
        },
        {"Image", null}
    };
    public static final String[] NAMES = new String[SHADERS.length];
    private static ShaderUtil current;
    private static int currentIndex;
    private static long initTime;
    private static int imageTextureId;

    private static int getImageTexture() {
        if (imageTextureId != -1) {
            return imageTextureId;
        }

        try {
            Minecraft mc = Minecraft.func_71410_x();
            InputStream is = mc.func_110442_L()
                .func_110536_a(new ResourceLocation("minecraft:miau/background/background.jpg"))
                .func_110527_b();
            BufferedImage img = ImageIO.read(is);
            is.close();
            if (img == null) {
                return -1;
            }

            int[] pixels = new int[img.getWidth() * img.getHeight()];
            img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());
            ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4).order(ByteOrder.nativeOrder());

            for (int pixel : pixels) {
                buffer.put((byte)(pixel >> 16 & 0xFF));
                buffer.put((byte)(pixel >> 8 & 0xFF));
                buffer.put((byte)(pixel & 0xFF));
                buffer.put((byte)(pixel >> 24 & 0xFF));
            }

            ((Buffer)buffer).flip();
            int texId = GL11.glGenTextures();
            GL11.glBindTexture(3553, texId);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexImage2D(3553, 0, 6408, img.getWidth(), img.getHeight(), 0, 6408, 5121, buffer);
            GL11.glBindTexture(3553, 0);
            imageTextureId = texId;
            return texId;
        } catch (Exception e) {
            return -1;
        }
    }

    public static void reload(int index) {
        if (index != SHADERS.length - 1) {
            if (index != currentIndex || current == null) {
                currentIndex = index;
                current = new ShaderUtil(SHADERS[index][1]);
                initTime = System.currentTimeMillis();
            }
        }
    }

    public static void draw(int screenW, int screenH, int shaderIndex) {
        if (shaderIndex == SHADERS.length - 1) {
            drawImage(screenW, screenH);
        } else {
            reload(shaderIndex);
            Minecraft mc = Minecraft.func_71410_x();
            if (current != null && current.getProgramID() != 0) {
                GlStateManager.func_179129_p();
                GlStateManager.func_179118_c();
                current.init();
                float t = (float)(System.currentTimeMillis() - initTime) / 1000.0F;
                current.setUniformf("time", t);
                current.setUniformf("resolution", mc.field_71443_c, mc.field_71440_d);
                Tessellator tess = Tessellator.func_178181_a();
                WorldRenderer wr = tess.func_178180_c();
                wr.func_181668_a(7, DefaultVertexFormats.field_181705_e);
                wr.func_181662_b(0.0, screenH, 0.0).func_181675_d();
                wr.func_181662_b(screenW, screenH, 0.0).func_181675_d();
                wr.func_181662_b(screenW, 0.0, 0.0).func_181675_d();
                wr.func_181662_b(0.0, 0.0, 0.0).func_181675_d();
                tess.func_78381_a();
                current.unload();
                GlStateManager.func_179141_d();
                GlStateManager.func_179089_o();
            } else {
                fallback(screenW, screenH);
            }
        }
    }

    private static void drawImage(int w, int h) {
        int texId = getImageTexture();
        if (texId == -1) {
            fallback(w, h);
        } else {
            GlStateManager.func_179129_p();
            GlStateManager.func_179098_w();
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179144_i(texId);
            Tessellator tess = Tessellator.func_178181_a();
            WorldRenderer wr = tess.func_178180_c();
            wr.func_181668_a(7, DefaultVertexFormats.field_181707_g);
            wr.func_181662_b(0.0, h, 0.0).func_181673_a(0.0, 1.0).func_181675_d();
            wr.func_181662_b(w, h, 0.0).func_181673_a(1.0, 1.0).func_181675_d();
            wr.func_181662_b(w, 0.0, 0.0).func_181673_a(1.0, 0.0).func_181675_d();
            wr.func_181662_b(0.0, 0.0, 0.0).func_181673_a(0.0, 0.0).func_181675_d();
            tess.func_78381_a();
            GlStateManager.func_179144_i(0);
            GlStateManager.func_179089_o();
        }
    }

    private static void fallback(int w, int h) {
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GlStateManager.func_179103_j(7425);
        Tessellator tess = Tessellator.func_178181_a();
        WorldRenderer wr = tess.func_178180_c();
        wr.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        wr.func_181662_b(w, 0.0, 0.0).func_181669_b(14, 14, 22, 255).func_181675_d();
        wr.func_181662_b(0.0, 0.0, 0.0).func_181669_b(14, 14, 22, 255).func_181675_d();
        wr.func_181662_b(0.0, h, 0.0).func_181669_b(5, 5, 12, 255).func_181675_d();
        wr.func_181662_b(w, h, 0.0).func_181669_b(5, 5, 12, 255).func_181675_d();
        tess.func_78381_a();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179084_k();
        GlStateManager.func_179098_w();
    }

    static {
        for (int i = 0; i < SHADERS.length; i++) {
            NAMES[i] = SHADERS[i][0];
        }

        currentIndex = -1;
        initTime = System.currentTimeMillis();
        imageTextureId = -1;
    }
}
