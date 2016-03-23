### UncommonRxAssignment

A tiny app that loads images from three sources:
- Memory cache
- Disk cache
- Internet

I initially did this by manually checking each of these 3 source and returning an Observable accordingly, but later replaced them with `Observable#amb`. This operator is new to me so my way may or may not be the correct way.

Start at [MainActivity#53](https://github.com/Saketme/UncommonRxAssignment/blob/master/app%2Fsrc%2Fmain%2Fjava%2Fme%2Fsaket%2Frxtest%2Fui%2FMainActivity.java#L53)

#### Video
https://www.youtube.com/watch?v=JLnsP36x3P0&feature=youtu.be
