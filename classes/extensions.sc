+ Synth{
	getOuts{|targetDictionary,synthdesclib=nil,list=nil|
		var sd,md,l,f;
		targetDictionary=targetDictionary??{()};
		sd=(synthdesclib??{SynthDescLib.global})[this.defName];
		md=sd!?{sd.metadata};
		if(list.isNil&&md.isNil){
			^Error("getOuts doesn't find list of keys send from the synth. These should be in the provided SynthDesc or in the provided list").throw;
		};
		l=list??{md[\outs]};
		f=l.collect{|name|
			OSCFunc({|msg|
				var v=msg[3..];
				switch(v.size
					,0,{v=nil}
					,1,{v=v.first}
				);
				if(targetDictionary[name].respondsTo(\value_)){
					if(targetDictionary[name].isView){
						{targetDictionary[name].value=v}.defer;
					}{
						targetDictionary[name].value=v;
					}
				}{
					if(targetDictionary[name].isFunction){
						targetDictionary[name].value(v);
					}{
						targetDictionary[name]=v;
					}
				}
			}
			, ("/my_set/"++name).asSymbol
			, this.server.addr
			, nil
			, [this.nodeID]);
		};
		OSCFunc({f.do({ arg c; c.free})},'/n_end', this.server.addr, nil,[this.nodeID]).oneShot;
		^targetDictionary;
	}
}

+ UGen{
	set{|name,val,list|
		var array=list??{UGen.buildSynthDef.metadata[\outs]};
		array=array.asArray.add(name.asSymbol);
		if(list.isNil){
			UGen.buildSynthDef.metadata[\outs]=array;
		}{
			list.array=array
		};
		switch(this.rate,
			\audio,{
				^SendReply.ar(this,("/my_set/"++name).asSymbol,val)
			},
			\control,{
				^SendReply.kr(this,("/my_set/"++name).asSymbol,val)
			},
			{
				^Error("didn't know when implementing UGen:set about the rate"+this.rate).throw
			}
		)
	}
}
