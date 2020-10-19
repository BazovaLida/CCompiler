.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Результат обчислень виразу", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

main PROC
	push 30
	pop EBX
	neg EBX
	push EBX

	pop EBX
	neg EBX
	push EBX

	push 3
	mov edx, 0
	pop ECX
	pop EAX
	idiv ECX
	push EAX

	pop eax ;here is the result

	ret
main ENDP

END start
