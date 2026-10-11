package xyz.daikirai.util;

import org.lwjgl.opengl.GL20;

public class Shader {
    public static final String VERTEX = "#version 120\n\nvoid main() {\n    gl_TexCoord[0] = gl_MultiTexCoord0;\n    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n}";

    public static final String KAWASE_DOWN = "#version 120\n\nuniform sampler2D inTexture;\nuniform vec2 offset, halfpixel, iResolution;\n\nvoid main() {\n    vec2 uv = gl_FragCoord.xy / iResolution;\n    vec4 sum = texture2D(inTexture, uv) * 4.0;\n    sum += texture2D(inTexture, uv - halfpixel.xy * offset);\n    sum += texture2D(inTexture, uv + halfpixel.xy * offset);\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n    sum += texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);\n    gl_FragColor = vec4(sum.rgb * 0.125, 1.0);\n}";

    public static final String KAWASE_UP = "#version 120\n\nuniform sampler2D inTexture, textureToCheck;\nuniform vec2 halfpixel, offset, iResolution;\nuniform int check;\n\nvoid main() {\n    vec2 uv = gl_FragCoord.xy / iResolution;\n    vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;\n    sum += texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;\n    sum += texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);\n    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;\n    vec4 average = sum / 12.0;\n    gl_FragColor = vec4(average.rgb, mix(1.0, texture2D(textureToCheck, gl_TexCoord[0].st).a, check));\n}";

    public int program;

    public Shader(String fragment) {
        this(fragment, VERTEX);
    }

    public Shader(String fragment, String vertex) {
        int f = GL20.glCreateShader(35632);
        int v = GL20.glCreateShader(35633);
        GL20.glShaderSource(f, fragment);
        GL20.glShaderSource(v, vertex);
        GL20.glCompileShader(f);
        GL20.glCompileShader(v);
        if (!this.compiled(f) || !this.compiled(v)) {
            this.program = -1;
            return;
        }
        this.program = GL20.glCreateProgram();
        GL20.glAttachShader(this.program, f);
        GL20.glAttachShader(this.program, v);
        GL20.glValidateProgram(this.program);
        GL20.glLinkProgram(this.program);
        GL20.glDeleteShader(f);
        GL20.glDeleteShader(v);
    }

    public boolean compiled(int n) {
        return GL20.glGetShaderi(n, 35713) == 1;
    }

    public void use() {
        GL20.glUseProgram(this.program);
    }

    public void stop() {
        GL20.glUseProgram(0);
    }

    public void uniformf(String name, float... v) {
        int loc = GL20.glGetUniformLocation(this.program, name);
        switch (v.length) {
            case 1: GL20.glUniform1f(loc, v[0]); break;
            case 2: GL20.glUniform2f(loc, v[0], v[1]); break;
            case 3: GL20.glUniform3f(loc, v[0], v[1], v[2]); break;
            case 4: GL20.glUniform4f(loc, v[0], v[1], v[2], v[3]);
        }
    }

    public void uniformi(String name, int v) {
        GL20.glUniform1i(GL20.glGetUniformLocation(this.program, name), v);
    }
}
