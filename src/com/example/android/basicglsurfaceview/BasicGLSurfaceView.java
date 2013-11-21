/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicglsurfaceview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.example.android.basicglsurfaceview.GLES20TriangleRenderer.VideoData;

class BasicGLSurfaceView extends GLSurfaceView {
	GLES20TriangleRenderer render;
    public BasicGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        render = new GLES20TriangleRenderer(context);
        setRenderer(render);
    }
    
    public BasicGLSurfaceView(Context context, AttributeSet attrs){
    	super(context, attrs);
    	setEGLContextClientVersion(2);
        render = new GLES20TriangleRenderer(context);
        setRenderer(render);
    }
    public VideoData getCurrentVideoData() {
    	return render.getCurrentData();
    }
}

